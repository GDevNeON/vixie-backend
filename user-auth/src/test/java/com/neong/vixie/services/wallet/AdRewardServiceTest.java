package com.neong.vixie.services.wallet;

import com.neong.vixie.models.constant.TransactionType;
import com.neong.vixie.models.db.CoinTransaction;
import com.neong.vixie.models.db.CoinWallet;
import com.neong.vixie.models.db.DailyCoinCap;
import com.neong.vixie.repositories.CoinTransactionRepository;
import com.neong.vixie.repositories.CoinWalletRepository;
import com.neong.vixie.repositories.DailyCoinCapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdRewardServiceTest {

    @Mock
    private DailyCoinCapRepository dailyCoinCapRepository;

    @Mock
    private CoinWalletRepository walletRepository;

    @Mock
    private CoinTransactionRepository transactionRepository;

    @InjectMocks
    private AdRewardService adRewardService;

    private DailyCoinCap dailyCap;
    private CoinWallet wallet;

    @BeforeEach
    void setUp() {
        dailyCap = DailyCoinCap.builder()
                .id("cap_1")
                .targetDate(LocalDate.now(ZoneOffset.UTC))
                .coinsEarned(0)
                .createdAt(Instant.now())
                .build();
                
        wallet = CoinWallet.builder()
                .userId("user_1")
                .balance(50)
                .build();
    }

    @Test
    void rewardAdWatch_underCap_grantsCoin() {
        when(dailyCoinCapRepository.findByUser_IdAndTargetDate(eq("user_1"), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyCap));
        when(walletRepository.findById("user_1")).thenReturn(Optional.of(wallet));
        
        when(dailyCoinCapRepository.save(any(DailyCoinCap.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(CoinWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(CoinTransaction.class))).thenAnswer(i -> i.getArgument(0));

        int newBalance = adRewardService.rewardAdWatch("user_1");

        assertEquals(51, newBalance);
        assertEquals(1, dailyCap.getCoinsEarned());
        
        verify(walletRepository).save(wallet);
        verify(dailyCoinCapRepository).save(dailyCap);
        verify(transactionRepository).save(argThat(tx -> 
            tx.getAmount() == 1 &&
            tx.getType() == TransactionType.CREDIT &&
            tx.getReason().equals("Ad reward")
        ));
    }

    @Test
    void rewardAdWatch_atCap_throwsException() {
        dailyCap.setCoinsEarned(10);
        when(dailyCoinCapRepository.findByUser_IdAndTargetDate(eq("user_1"), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyCap));

        assertThrows(AdCapExceededException.class, () -> {
            adRewardService.rewardAdWatch("user_1");
        });

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
