package com.neong.vixie.services.wallet;

import com.neong.vixie.exceptions.InsufficientFundsException;
import com.neong.vixie.models.constant.TransactionType;
import com.neong.vixie.models.db.CoinTransaction;
import com.neong.vixie.models.db.CoinWallet;
import com.neong.vixie.models.db.GachaPityTracker;
import com.neong.vixie.models.db.GachaPullRecord;
import com.neong.vixie.models.db.UserInventory;
import com.neong.vixie.repositories.CoinTransactionRepository;
import com.neong.vixie.repositories.CoinWalletRepository;
import com.neong.vixie.repositories.GachaPityTrackerRepository;
import com.neong.vixie.repositories.GachaPullRecordRepository;
import com.neong.vixie.repositories.UserInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GachaInternalServiceTest {

    @Mock
    private CoinWalletRepository walletRepository;

    @Mock
    private CoinTransactionRepository transactionRepository;

    @Mock
    private UserInventoryRepository inventoryRepository;

    @Mock
    private GachaPityTrackerRepository pityRepository;

    @Mock
    private GachaPullRecordRepository pullRecordRepository;

    @InjectMocks
    private GachaInternalService gachaInternalService;

    private CoinWallet wallet;
    private GachaPityTracker pityTracker;

    @BeforeEach
    void setUp() {
        wallet = CoinWallet.builder()
                .userId("user_1")
                .balance(100)
                .build();

        pityTracker = GachaPityTracker.builder()
                .id("pity_1")
                .bannerId("banner_1")
                .currentPity(5)
                .build();
    }

    @Test
    void commitPulls_success_deductsCoinsAndRecords() {
        when(walletRepository.findByIdForUpdate("user_1")).thenReturn(Optional.of(wallet));
        when(pityRepository.findByUser_IdAndBannerIdForUpdate("user_1", "banner_1"))
                .thenReturn(Optional.of(pityTracker));

        when(inventoryRepository.existsByUser_IdAndItemId("user_1", "item_1")).thenReturn(false);

        List<GachaInternalService.PullResultEntry> results = List.of(
                new GachaInternalService.PullResultEntry("item_1", "COMMON")
        );

        int newBalance = gachaInternalService.commitPulls("user_1", "banner_1", 5, results);

        assertEquals(95, newBalance);
        assertEquals(6, pityTracker.getCurrentPity());

        verify(walletRepository).save(wallet);
        verify(transactionRepository).save(argThat(tx ->
                tx.getAmount() == 5 &&
                tx.getType() == TransactionType.DEBIT
        ));
        verify(inventoryRepository).save(any(UserInventory.class));
        verify(pullRecordRepository).save(any(GachaPullRecord.class));
        verify(pityRepository).save(pityTracker);
    }

    @Test
    void commitPulls_insufficientFunds_throwsException() {
        when(walletRepository.findByIdForUpdate("user_1")).thenReturn(Optional.of(wallet));

        List<GachaInternalService.PullResultEntry> results = List.of(
                new GachaInternalService.PullResultEntry("item_1", "COMMON")
        );

        assertThrows(InsufficientFundsException.class, () -> {
            gachaInternalService.commitPulls("user_1", "banner_1", 150, results);
        });

        verify(walletRepository, never()).save(any());
    }

    @Test
    void commitPulls_epicResetsPity() {
        when(walletRepository.findByIdForUpdate("user_1")).thenReturn(Optional.of(wallet));
        when(pityRepository.findByUser_IdAndBannerIdForUpdate("user_1", "banner_1"))
                .thenReturn(Optional.of(pityTracker));

        List<GachaInternalService.PullResultEntry> results = List.of(
                new GachaInternalService.PullResultEntry("item_1", "EPIC")
        );

        gachaInternalService.commitPulls("user_1", "banner_1", 5, results);

        assertEquals(0, pityTracker.getCurrentPity());
    }
}
