package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);



    List<Product> findByOwnerIdIsNullOrOwnerId(Integer ownerId);

    boolean existsByBarcodeAndOwnerIdIsNull(String barcode);
    boolean existsByBarcodeAndOwnerId(String barcode, Integer ownerId);

    List<Product> findByNameContainingIgnoreCaseAndOwnerIdIsNull(String name);
    List<Product> findByNameContainingIgnoreCaseAndOwnerId(String name, Integer ownerId);

    List<Product> findByNameIgnoreCaseAndOwnerIdIsNull(String name);
    List<Product> findByNameIgnoreCaseAndOwnerId(String name, Integer ownerId);
    Optional<Product> findByBarcodeAndOwnerIdIsNull(String barcode);
    Optional<Product> findByBarcodeAndOwnerId(String barcode, Integer userId);
}
