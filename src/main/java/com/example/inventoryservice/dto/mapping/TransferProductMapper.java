package com.example.inventoryservice.dto.mapping;


import com.example.inventoryservice.dto.TransferProductDto;
import com.example.inventoryservice.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface TransferProductMapper {

    @Mapping(target = "instances", ignore = true)
    void updateFromEntity(Product product, @MappingTarget TransferProductDto transferProductDto);
}
