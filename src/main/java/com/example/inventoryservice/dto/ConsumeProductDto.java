package com.example.inventoryservice.dto;

import com.example.inventoryservice.enums.Measure;
import lombok.Data;

@Data
public class ConsumeProductDto {
    private String productName;
    private double amount;
    private Measure unit;
    private Integer userId;
}