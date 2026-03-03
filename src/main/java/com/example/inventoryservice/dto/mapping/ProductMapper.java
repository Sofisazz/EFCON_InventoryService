package com.example.inventoryservice.dto.mapping;

import com.example.inventoryservice.dto.ProductDto;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ProductInstanceMapper.class})
public interface ProductMapper {

    ProductDto toDto(Product product);
    Product toEntity(ProductDto productDto);

    @AfterMapping
    default void linkProductInstancesWithProduct(@MappingTarget Product product) {
        for (ProductInstance productInstance : product.getInstances()) {
            productInstance.setProduct(product);
        }
    }
}
