package com.neong.vixie.services.wallet;

import com.neong.vixie.exceptions.InsufficientFundsException;
import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.constant.TransactionType;
import com.neong.vixie.models.db.CoinTransaction;
import com.neong.vixie.models.db.CoinWallet;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserInventory;
import com.neong.vixie.models.dto.PurchaseResponse;
import com.neong.vixie.repositories.CoinTransactionRepository;
import com.neong.vixie.repositories.CoinWalletRepository;
import com.neong.vixie.repositories.UserInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoinWalletService {

    private static final Logger log = LoggerFactory.getLogger(CoinWalletService.class);

    private final CoinWalletRepository walletRepository;
    private final CoinTransactionRepository transactionRepository;
    private final UserInventoryRepository inventoryRepository;
    private final RestClient aiCompanionRestClient;

    @Value("${api.internal.key:default-internal-key-123}")
    private String internalApiKey;

    /**
     * Get or create wallet for user. Creates with 0 balance if not exists.
     */
    public int getBalance(String userId) {
        return walletRepository.findById(userId)
                .map(CoinWallet::getBalance)
                .orElse(0);
    }

    /**
     * Grant coins to a user (admin operation).
     */
    @Transactional
    public int grantCoins(String userId, int amount, String reason) {
        CoinWallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        CoinTransaction transaction = CoinTransaction.builder()
                .id(IdGenerator.generateId("coin_tx"))
                .user(User.builder().id(userId).build())
                .amount(amount)
                .type(TransactionType.CREDIT)
                .reason(reason)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(transaction);

        log.info("Granted {} coins to user {} for reason: {}", amount, userId, reason);
        return wallet.getBalance();
    }

    /**
     * Atomic purchase flow:
     * 1. Validate item exists + price matches via ai-companion
     * 2. Deduct coins (optimistic lock prevents double-spend)
     * 3. Record transaction
     * 4. Add to inventory
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public PurchaseResponse purchaseItem(String userId, String itemId, int expectedPriceCoins) {
        // Step 1: Validate item via ai-companion
        Map<String, Object> item;
        try {
            item = aiCompanionRestClient.get()
                    .uri("/api/marketplace/items/{id}", itemId)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Item not found: {}", itemId);
            throw new IllegalArgumentException("Item not found: " + itemId);
        } catch (Exception e) {
            log.error("Failed to validate item {} from ai-companion", itemId, e);
            throw new IllegalArgumentException("Unable to validate item. Please try again.");
        }

        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        Integer actualPrice = (Integer) item.get("price_coins");
        if (actualPrice == null) {
            throw new IllegalArgumentException("Item is not available for coin purchase: " + itemId);
        }
        if (!actualPrice.equals(expectedPriceCoins)) {
            throw new IllegalArgumentException(
                    "Price mismatch: expected " + expectedPriceCoins + " but item costs " + actualPrice);
        }

        // Check if already owned
        if (inventoryRepository.existsByUser_IdAndItemId(userId, itemId)) {
            throw new IllegalArgumentException("You already own this item");
        }

        // Step 2: Deduct coins
        CoinWallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance() < expectedPriceCoins) {
            throw new InsufficientFundsException(wallet.getBalance(), expectedPriceCoins);
        }

        wallet.setBalance(wallet.getBalance() - expectedPriceCoins);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        // Step 3: Record transaction
        CoinTransaction transaction = CoinTransaction.builder()
                .id(IdGenerator.generateId("coin_tx"))
                .user(User.builder().id(userId).build())
                .amount(expectedPriceCoins)
                .type(TransactionType.DEBIT)
                .reason("Purchase: " + itemId)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(transaction);

        // Step 4: Add to inventory
        UserInventory inventory = UserInventory.builder()
                .id(IdGenerator.generateId("inv"))
                .user(User.builder().id(userId).build())
                .itemId(itemId)
                .acquiredAt(Instant.now())
                .build();
        inventoryRepository.save(inventory);

        log.info("User {} purchased item {} for {} coins", userId, itemId, expectedPriceCoins);

        // Fire-and-forget: notify ai-companion for creator revenue tracking
        // This MUST NOT rollback the purchase if it fails
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    aiCompanionRestClient.post()
                            .uri("/api/internal/purchase-event")
                            .header("X-Service-Key", internalApiKey)
                            .body(Map.of(
                                    "item_id", itemId,
                                    "buyer_user_id", userId,
                                    "price_coins", expectedPriceCoins,
                                    "purchased_at", Instant.now().toString()
                            ))
                            .retrieve()
                            .toBodilessEntity();
                    log.info("Purchase event sent to ai-companion for item {}", itemId);
                } catch (Exception e) {
                    log.warn("Fire-and-forget purchase event failed for item {}: {}", itemId, e.getMessage());
                }
            }
        });

        return new PurchaseResponse(true, wallet.getBalance(), itemId);
    }

    private CoinWallet getOrCreateWallet(String userId) {
        return walletRepository.findById(userId)
                .orElseGet(() -> {
                    try {
                        CoinWallet newWallet = CoinWallet.builder()
                                .userId(userId)
                                .balance(0)
                                .createdAt(Instant.now())
                                .build();
                        return walletRepository.save(newWallet);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        return walletRepository.findById(userId)
                                .orElseThrow(() -> new IllegalStateException("Failed to retrieve wallet after creation collision"));
                    }
                });
    }
}
