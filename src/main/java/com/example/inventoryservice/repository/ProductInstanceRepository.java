package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.ProductInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductInstanceRepository extends JpaRepository<ProductInstance, Integer> {

    List<ProductInstance> findByProductId(int productId);
    Optional<ProductInstance> findByIdAndProductId(int id, int productId);
    boolean existsByIdAndProductId(int id, int productId);
}
