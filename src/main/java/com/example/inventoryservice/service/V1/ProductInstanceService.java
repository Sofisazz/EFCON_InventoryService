package com.example.inventoryservice.service.V1;

import com.example.inventoryservice.dto.ProductInstanceDto;

import java.util.List;

public interface ProductInstanceService {

    List<ProductInstanceDto> findAllProductInstances();
    ProductInstanceDto findProductInstanceById(int id);
    ProductInstanceDto createProductInstance(ProductInstanceDto productInstanceDto);
    ProductInstanceDto updateProductInstance(int id,ProductInstanceDto productInstanceDto);
    void deleteProductInstanceById(int id);
}
