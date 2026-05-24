package com.neong.vixie.services.wallet;

import com.neong.vixie.exceptions.InsufficientFundsException;
import com.neong.vixie.models.constant.TransactionType;
import com.neong.vixie.models.db.CoinTransaction;
import com.neong.vixie.models.db.CoinWallet;
import com.neong.vixie.models.db.UserInventory;
import com.neong.vixie.models.dto.PurchaseResponse;
import com.neong.vixie.repositories.CoinTransactionRepository;
import com.neong.vixie.repositories.CoinWalletRepository;
import com.neong.vixie.repositories.UserInventoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseFlowTest {

    @Mock
    private CoinWalletRepository walletRepository;

    @Mock
    private CoinTransactionRepository transactionRepository;

    @Mock
    private UserInventoryRepository inventoryRepository;

    @Mock
    private RestClient aiCompanionRestClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private CoinWalletService coinWalletService;

    private CoinWallet testWallet;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        
        testWallet = CoinWallet.builder()
                .userId("user_1")
                .balance(150)
                .createdAt(Instant.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void purchaseItem_success_deductsCoinsAndAddsToInventory() {
        // Mock rest client chain
        when(aiCompanionRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("price_coins", 100));

        when(inventoryRepository.existsByUser_IdAndItemId("user_1", "item_1")).thenReturn(false);
        when(walletRepository.findById("user_1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(CoinWallet.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseResponse response = coinWalletService.purchaseItem("user_1", "item_1", 100);

        assertTrue(response.success());
        assertEquals(50, response.newBalance());
        assertEquals("item_1", response.itemId());

        verify(walletRepository).save(testWallet);
        assertEquals(50, testWallet.getBalance());
        
        verify(transactionRepository).save(argThat(tx -> 
            tx.getAmount() == 100 &&
            tx.getType() == TransactionType.DEBIT
        ));
        
        verify(inventoryRepository).save(argThat(inv -> 
            inv.getItemId().equals("item_1") &&
            inv.getUser().getId().equals("user_1")
        ));
    }

    @Test
    void purchaseItem_insufficientFunds_throwsException() {
        when(aiCompanionRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("price_coins", 200)); // Expected is 200

        when(inventoryRepository.existsByUser_IdAndItemId("user_1", "item_1")).thenReturn(false);
        when(walletRepository.findById("user_1")).thenReturn(Optional.of(testWallet)); // Balance is 150

        assertThrows(InsufficientFundsException.class, () -> 
            coinWalletService.purchaseItem("user_1", "item_1", 200)
        );

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    void purchaseItem_priceMismatch_throwsException() {
        when(aiCompanionRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("price_coins", 200)); // Actual is 200

        assertThrows(IllegalArgumentException.class, () -> 
            coinWalletService.purchaseItem("user_1", "item_1", 100) // Expected is 100
        );
    }
    
    @Test
    void purchaseItem_alreadyOwned_throwsException() {
        when(aiCompanionRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("price_coins", 100));

        when(inventoryRepository.existsByUser_IdAndItemId("user_1", "item_1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> 
            coinWalletService.purchaseItem("user_1", "item_1", 100)
        );
    }
}
