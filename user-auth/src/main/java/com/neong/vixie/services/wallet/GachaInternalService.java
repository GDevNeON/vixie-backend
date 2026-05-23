package com.neong.vixie.services.wallet;

import com.neong.vixie.exceptions.InsufficientFundsException;
import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.constant.TransactionType;
import com.neong.vixie.models.db.CoinTransaction;
import com.neong.vixie.models.db.CoinWallet;
import com.neong.vixie.models.db.GachaPityTracker;
import com.neong.vixie.models.db.GachaPullRecord;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserInventory;
import com.neong.vixie.repositories.CoinTransactionRepository;
import com.neong.vixie.repositories.CoinWalletRepository;
import com.neong.vixie.repositories.GachaPityTrackerRepository;
import com.neong.vixie.repositories.GachaPullRecordRepository;
import com.neong.vixie.repositories.UserInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Internal service for gacha pull commits. Called by ai-companion via internal API.
 * Handles: coin deduction, inventory add, pity tracking, pull record, all atomically.
 */
@Service
@RequiredArgsConstructor
public class GachaInternalService {

    private static final Logger log = LoggerFactory.getLogger(GachaInternalService.class);

    private final CoinWalletRepository walletRepository;
    private final CoinTransactionRepository transactionRepository;
    private final UserInventoryRepository inventoryRepository;
    private final GachaPityTrackerRepository pityRepository;
    private final GachaPullRecordRepository pullRecordRepository;

    /**
     * Get current pity count for user+banner.
     */
    public int getPityCount(String userId, String bannerId) {
        return pityRepository.findByUser_IdAndBannerId(userId, bannerId)
                .map(GachaPityTracker::getCurrentPity)
                .orElse(0);
    }

    /**
     * Commit a batch of gacha pull results atomically.
     * Deducts total cost, adds items to inventory, updates pity, records pulls.
     *
     * @param userId     authenticated user
     * @param bannerId   banner pulled from
     * @param totalCost  total coins to deduct
     * @param pullResults list of (itemId, rarity) pairs
     * @return new wallet balance
     */
    @Transactional
    public int commitPulls(String userId, String bannerId, int totalCost,
                           List<PullResultEntry> pullResults) {
        // 1. Deduct coins
        CoinWallet wallet = walletRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new InsufficientFundsException(0, totalCost));

        if (wallet.getBalance() < totalCost) {
            throw new InsufficientFundsException(wallet.getBalance(), totalCost);
        }

        wallet.setBalance(wallet.getBalance() - totalCost);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        // 2. Record debit transaction
        CoinTransaction tx = CoinTransaction.builder()
                .id(IdGenerator.generateId("coin_tx"))
                .user(User.builder().id(userId).build())
                .amount(totalCost)
                .type(TransactionType.DEBIT)
                .reason("Gacha pull: banner " + bannerId)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(tx);

        // 3. Process each pull result
        GachaPityTracker pity = pityRepository.findByUser_IdAndBannerIdForUpdate(userId, bannerId)
                .orElseGet(() -> {
                    GachaPityTracker newPity = GachaPityTracker.builder()
                            .id(IdGenerator.generateId("pity"))
                            .user(User.builder().id(userId).build())
                            .bannerId(bannerId)
                            .currentPity(0)
                            .createdAt(Instant.now())
                            .build();
                    return pityRepository.save(newPity);
                });

        int costPerPull = pullResults.size() > 0 ? totalCost / pullResults.size() : 0;

        for (PullResultEntry entry : pullResults) {
            // Add to inventory (skip if already owned)
            if (!inventoryRepository.existsByUser_IdAndItemId(userId, entry.itemId())) {
                UserInventory inv = UserInventory.builder()
                        .id(IdGenerator.generateId("inv"))
                        .user(User.builder().id(userId).build())
                        .itemId(entry.itemId())
                        .acquiredAt(Instant.now())
                        .build();
                inventoryRepository.save(inv);
            }

            // Record pull
            GachaPullRecord record = GachaPullRecord.builder()
                    .id(IdGenerator.generateId("pull"))
                    .user(User.builder().id(userId).build())
                    .bannerId(bannerId)
                    .itemId(entry.itemId())
                    .rarity(entry.rarity())
                    .pullCost(costPerPull)
                    .pulledAt(Instant.now())
                    .build();
            pullRecordRepository.save(record);

            // Update pity
            if ("EPIC".equalsIgnoreCase(entry.rarity())) {
                pity.setCurrentPity(0); // Reset on Epic
            } else if (!"LIMITED".equalsIgnoreCase(entry.rarity())) {
                pity.setCurrentPity(pity.getCurrentPity() + 1);
            }
            // Limited items do not affect pity counter
        }

        pity.setUpdatedAt(Instant.now());
        pityRepository.save(pity);

        log.info("Gacha commit: user={}, banner={}, pulls={}, cost={}, newBalance={}",
                userId, bannerId, pullResults.size(), totalCost, wallet.getBalance());

        return wallet.getBalance();
    }

    /**
     * DTO for a single pull result entry.
     */
    public record PullResultEntry(String itemId, String rarity) {}
}
