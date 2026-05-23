package com.neong.vixie.repositories;

import com.neong.vixie.models.db.CoinWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoinWalletRepository extends JpaRepository<CoinWallet, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CoinWallet c WHERE c.id = :id")
    Optional<CoinWallet> findByIdForUpdate(@Param("id") String id);
}
