package com.example.inventoryservice.service.V2;

import com.example.inventoryservice.dto.ProductAvailabilityDto;
import com.example.inventoryservice.dto.ProductDto;
import com.example.inventoryservice.dto.ProductStatusDto;
import com.example.inventoryservice.enums.Measure;

import java.util.List;

public interface ProductServiceV2 {

    List<ProductAvailabilityDto> checkProductsAvailability(Integer userId, List<String> productNames);

    List<ProductStatusDto> generateShoppingList(Integer userId, List<String> productNames);

    ProductDto createProduct(ProductDto productDto, Integer userId);

    List<ProductDto> findAllProducts(Integer userId);

    List<ProductDto> findProductsByName(String name, Integer userId);

    void deleteProductById(Integer id, Integer userId);

    ProductDto findOrCreateByBarcode(String barcode, Integer userId);

    ProductDto updateProduct(int id, ProductDto productDto, Integer userId);

    void consumeProduct(int userId, String productName, double amount, Measure unit);

    void returnProduct(int userId, String productName, double amount, Measure unit);
}
