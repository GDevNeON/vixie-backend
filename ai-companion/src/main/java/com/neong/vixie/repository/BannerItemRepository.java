package com.neong.vixie.repository;

import com.neong.vixie.model.BannerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerItemRepository extends JpaRepository<BannerItem, String> {

    List<BannerItem> findByBanner_Id(String bannerId);
}
