package com.example.inventoryservice.dto.mapping;

import com.example.inventoryservice.dto.ProductInstanceDto;
import com.example.inventoryservice.entity.ProductInstance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface ProductInstanceMapper {

    @Mapping(target = "productId", expression = "java(productInstance.getProduct().getId())")
    ProductInstanceDto toDto(ProductInstance productInstance);

    ProductInstance toEntity(ProductInstanceDto productInstanceDto);
}
