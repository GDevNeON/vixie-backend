package com.neong.vixie.repositories;

import com.neong.vixie.models.db.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, String> {

    List<CoinTransaction> findByUser_IdOrderByCreatedAtDesc(String userId);
}
