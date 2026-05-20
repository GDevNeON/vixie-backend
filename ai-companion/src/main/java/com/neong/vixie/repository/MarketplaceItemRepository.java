package com.neong.vixie.repository;

import com.neong.vixie.model.ContentStatus;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.model.Rarity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, String> {

    Page<MarketplaceItem> findByStatus(ContentStatus status, Pageable pageable);

    Page<MarketplaceItem> findByStatusAndRarity(ContentStatus status, Rarity rarity, Pageable pageable);

    List<MarketplaceItem> findByCreatorId(String creatorId);
}
