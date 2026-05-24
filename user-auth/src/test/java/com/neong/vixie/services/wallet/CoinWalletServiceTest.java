package com.neong.vixie.services.wallet;

import com.neong.vixie.models.constant.TransactionType;
import com.neong.vixie.models.db.CoinTransaction;
import com.neong.vixie.models.db.CoinWallet;
import com.neong.vixie.repositories.CoinTransactionRepository;
import com.neong.vixie.repositories.CoinWalletRepository;
import com.neong.vixie.repositories.UserInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinWalletServiceTest {

    @Mock
    private CoinWalletRepository walletRepository;

    @Mock
    private CoinTransactionRepository transactionRepository;

    @Mock
    private UserInventoryRepository inventoryRepository;

    @Mock
    private RestClient aiCompanionRestClient;

    @InjectMocks
    private CoinWalletService coinWalletService;

    private CoinWallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = CoinWallet.builder()
                .userId("user_1")
                .balance(50)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getBalance_existingWallet_returnsBalance() {
        when(walletRepository.findById("user_1")).thenReturn(Optional.of(testWallet));
        int balance = coinWalletService.getBalance("user_1");
        assertEquals(50, balance);
    }

    @Test
    void getBalance_noWallet_returnsZero() {
        when(walletRepository.findById("user_2")).thenReturn(Optional.empty());
        int balance = coinWalletService.getBalance("user_2");
        assertEquals(0, balance);
    }

    @Test
    void grantCoins_existingWallet_addsBalanceAndRecordsTransaction() {
        when(walletRepository.findById("user_1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(CoinWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(CoinTransaction.class))).thenAnswer(i -> i.getArgument(0));

        int newBalance = coinWalletService.grantCoins("user_1", 100, "Admin grant");

        assertEquals(150, newBalance);
        assertEquals(150, testWallet.getBalance());
        
        verify(walletRepository).save(testWallet);
        verify(transactionRepository).save(argThat(tx -> 
            tx.getAmount() == 100 &&
            tx.getType() == TransactionType.CREDIT &&
            tx.getReason().equals("Admin grant")
        ));
    }
    
    @Test
    void grantCoins_noWallet_createsWalletAndAddsBalance() {
        when(walletRepository.findById("user_2"))
            .thenReturn(Optional.empty()) // first lookup fails
            .thenReturn(Optional.empty()); // getOrCreateWallet lookup fails
            
        when(walletRepository.save(any(CoinWallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(CoinTransaction.class))).thenAnswer(i -> i.getArgument(0));

        int newBalance = coinWalletService.grantCoins("user_2", 100, "Admin grant");

        assertEquals(100, newBalance);
        verify(walletRepository, times(2)).save(any(CoinWallet.class)); // 1 for creation, 1 for update
    }
}
