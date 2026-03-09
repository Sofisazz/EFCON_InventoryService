package com.example.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDataDto {
    @JsonProperty("product_name_ru")
    private String productNameRu;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("generic_name")
    private String genericName;

    private String brands;
    private String categories;

    private NutrimentsDto nutriments;
}