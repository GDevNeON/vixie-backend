package com.neong.vixie.repositories;

import com.neong.vixie.models.db.GachaPullRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GachaPullRecordRepository extends JpaRepository<GachaPullRecord, String> {

    Page<GachaPullRecord> findByUser_IdAndBannerIdOrderByPulledAtDesc(
            String userId, String bannerId, Pageable pageable);

    Page<GachaPullRecord> findByUser_IdOrderByPulledAtDesc(String userId, Pageable pageable);
}
