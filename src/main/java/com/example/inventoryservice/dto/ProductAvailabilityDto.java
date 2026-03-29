package com.example.inventoryservice.dto;

import lombok.Data;

@Data
public class ProductAvailabilityDto {
    private String productName;
    private boolean available;
    private String message;
}