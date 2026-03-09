package com.example.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NutrimentsDto {
    @JsonProperty("energy-kcal")
    private double energyKcal;

    private double proteins;
    private double fat;
    private double carbohydrates;
    private double salt;
    private double sugars;
}