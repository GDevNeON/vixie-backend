package com.neong.vixie.services.wallet;

import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.constant.TransactionType;
import com.neong.vixie.models.db.CoinTransaction;
import com.neong.vixie.models.db.CoinWallet;
import com.neong.vixie.models.db.DailyCoinCap;
import com.neong.vixie.models.db.User;
import com.neong.vixie.repositories.CoinTransactionRepository;
import com.neong.vixie.repositories.CoinWalletRepository;
import com.neong.vixie.repositories.DailyCoinCapRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AdRewardService {

    private static final Logger log = LoggerFactory.getLogger(AdRewardService.class);
    private static final int MAX_DAILY_AD_COINS = 10;
    private static final int COINS_PER_AD = 1;

    private final DailyCoinCapRepository dailyCoinCapRepository;
    private final CoinWalletRepository walletRepository;
    private final CoinTransactionRepository transactionRepository;

    /**
     * Grant 1 coin for watching an ad. Returns new balance.
     * @throws AdCapExceededException if daily cap (10) reached
     */
    @Transactional
    public int rewardAdWatch(String userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        DailyCoinCap cap = dailyCoinCapRepository.findByUser_IdAndTargetDate(userId, today)
                .orElseGet(() -> {
                    DailyCoinCap newCap = DailyCoinCap.builder()
                            .id(IdGenerator.generateId("ad_cap"))
                            .user(User.builder().id(userId).build())
                            .targetDate(today)
                            .coinsEarned(0)
                            .createdAt(Instant.now())
                            .build();
                    return dailyCoinCapRepository.save(newCap);
                });

        if (cap.getCoinsEarned() >= MAX_DAILY_AD_COINS) {
            throw new AdCapExceededException(cap.getCoinsEarned(), MAX_DAILY_AD_COINS);
        }

        // Update daily cap
        cap.setCoinsEarned(cap.getCoinsEarned() + COINS_PER_AD);
        cap.setUpdatedAt(Instant.now());
        dailyCoinCapRepository.save(cap);

        // Credit wallet
        CoinWallet wallet = walletRepository.findById(userId)
                .orElseGet(() -> {
                    CoinWallet newWallet = CoinWallet.builder()
                            .userId(userId)
                            .balance(0)
                            .createdAt(Instant.now())
                            .build();
                    return walletRepository.save(newWallet);
                });

        wallet.setBalance(wallet.getBalance() + COINS_PER_AD);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        // Record transaction
        CoinTransaction tx = CoinTransaction.builder()
                .id(IdGenerator.generateId("coin_tx"))
                .user(User.builder().id(userId).build())
                .amount(COINS_PER_AD)
                .type(TransactionType.CREDIT)
                .reason("Ad reward")
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(tx);

        log.info("Ad reward: user {} earned {} coin (daily total: {})",
                userId, COINS_PER_AD, cap.getCoinsEarned());

        return wallet.getBalance();
    }

    public int getRemainingAdWatches(String userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int earned = dailyCoinCapRepository.findByUser_IdAndTargetDate(userId, today)
                .map(DailyCoinCap::getCoinsEarned)
                .orElse(0);
        return Math.max(0, MAX_DAILY_AD_COINS - earned);
    }
}
