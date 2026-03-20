package com.example.inventoryservice.dto;

import com.example.inventoryservice.enums.Categories;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TransferProductDto {
    private int id;
    private String name;
    private Categories category;
    private String barcode;
    private List<ProductInstanceDto> instances = new ArrayList<>();
    private String brand;

    private double calories;
    private double proteins;
    private double fats;
    private double carbohydrates;
    private LocalDate expirationDate;
    public void addInstance(ProductInstanceDto productInstance) {
        instances.add(productInstance);
    }
}
