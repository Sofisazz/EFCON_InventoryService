package com.example.inventoryservice.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductInstanceDto {
    private int id;
    private int count;
    private LocalDateTime createdAt;
    private LocalDate expirationDate;
    private int productId;
}