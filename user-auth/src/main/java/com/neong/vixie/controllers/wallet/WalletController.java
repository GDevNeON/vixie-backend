package com.neong.vixie.controllers.wallet;

import com.neong.vixie.models.dto.GrantCoinsRequest;
import com.neong.vixie.models.dto.PurchaseRequest;
import com.neong.vixie.models.dto.PurchaseResponse;
import com.neong.vixie.models.dto.WalletBalanceResponse;
import com.neong.vixie.services.wallet.CoinWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WalletController {

    private final CoinWalletService coinWalletService;

    @GetMapping("/api/wallet/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(
            @AuthenticationPrincipal UserDetails user) {
        int balance = coinWalletService.getBalance(getUserId(user));
        return ResponseEntity.ok(new WalletBalanceResponse(balance));
    }

    @PostMapping("/api/wallet/purchase")
    public ResponseEntity<PurchaseResponse> purchase(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PurchaseRequest request) {
        PurchaseResponse response = coinWalletService.purchaseItem(
                getUserId(user),
                request.itemId(),
                request.expectedPriceCoins());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/wallet/grant-coins")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<WalletBalanceResponse> grantCoins(
            @Valid @RequestBody GrantCoinsRequest request) {
        int newBalance = coinWalletService.grantCoins(
                request.userId(),
                request.amount(),
                request.reason());
        return ResponseEntity.ok(new WalletBalanceResponse(newBalance));
    }

    private String getUserId(UserDetails user) {
        if (user instanceof com.neong.vixie.models.db.User u) {
            return u.getId();
        }
        throw new IllegalStateException("Unexpected UserDetails type");
    }
}
