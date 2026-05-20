package com.neong.vixie.repository;

import com.neong.vixie.model.CreatorSaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreatorSaleRecordRepository extends JpaRepository<CreatorSaleRecord, String> {

    List<CreatorSaleRecord> findByCreatorId(String creatorId);

    List<CreatorSaleRecord> findByItemId(String itemId);
}
