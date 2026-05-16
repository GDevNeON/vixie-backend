package com.neong.vixie.repositories;

import com.neong.vixie.models.db.UserInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInventoryRepository extends JpaRepository<UserInventory, String> {

    List<UserInventory> findByUser_Id(String userId);

    boolean existsByUser_IdAndItemId(String userId, String itemId);
}
