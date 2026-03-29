package com.example.inventoryservice.service.V2;

import com.example.inventoryservice.dto.ProductInstanceDto;
import com.example.inventoryservice.dto.TransferProductDto;

import java.util.List;

public interface ProductInstanceServiceV2 {

    ProductInstanceDto createProductInstanceForUser(ProductInstanceDto productInstanceDto, Integer userId);

    List<ProductInstanceDto> findInstancesForUser(Integer userId);

    ProductInstanceDto findProductInstanceById(int id,Integer userId);

    ProductInstanceDto updateProductInstance(int id, Integer userId, ProductInstanceDto productInstanceDto);

    void deleteProductInstanceById(int id, Integer userId);

    List<TransferProductDto> getExpiring(Integer userId);
}
