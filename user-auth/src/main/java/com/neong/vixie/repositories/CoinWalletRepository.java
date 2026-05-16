package com.neong.vixie.repositories;

import com.neong.vixie.models.db.CoinWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoinWalletRepository extends JpaRepository<CoinWallet, String> {
}
