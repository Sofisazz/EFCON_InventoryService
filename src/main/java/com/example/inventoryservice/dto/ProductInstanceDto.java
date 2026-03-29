package com.example.inventoryservice.dto;

import com.example.inventoryservice.enums.Measure;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductInstanceDto {
    private int id;
    private double count;
    private Measure unit;
    private LocalDateTime createdAt;
    private LocalDate expirationDate;
    private int productId;

    private Integer userId;
}