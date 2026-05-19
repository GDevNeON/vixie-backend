package com.neong.vixie.repository;

import com.neong.vixie.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, String> {

    @Query("SELECT b FROM Banner b WHERE b.isActive = true AND b.startDate <= :now AND b.endDate >= :now")
    List<Banner> findActiveBanners(Instant now);
}
