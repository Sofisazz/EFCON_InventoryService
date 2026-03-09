package com.example.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodDto {
    private String status;
    private ProductDataDto product;
}