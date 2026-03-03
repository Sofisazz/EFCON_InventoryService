package com.example.inventoryservice.dto;

import com.example.inventoryservice.enums.Categories;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDto {
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
}
