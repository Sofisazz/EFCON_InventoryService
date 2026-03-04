package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.ProductDto;
import com.example.inventoryservice.dto.ProductInstanceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProductService {

    Page<ProductDto> findAllProducts(Pageable pageable);
    ProductDto findProductById(int id);
    ProductDto createProduct(ProductDto productDto);
    ProductDto updateProduct(int id,ProductDto productDto);
    void deleteProductById(int id);
    List<ProductInstanceDto> findAllInstancesForProductById(int id);
    ProductInstanceDto findInstanceForProductById(int idPr, int idInst);
    ProductInstanceDto createInstanceForProductById(int id, ProductInstanceDto productInstanceDto);
    void deleteInstanceForProductById(int idPr, int idInst);
    ProductDto findOrCreateByBarcode(String barcode);
}
