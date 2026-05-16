package com.neong.vixie.repository;

import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.model.Rarity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, String> {

    Page<MarketplaceItem> findByIsActiveTrue(Pageable pageable);

    Page<MarketplaceItem> findByIsActiveTrueAndRarity(Rarity rarity, Pageable pageable);
}
