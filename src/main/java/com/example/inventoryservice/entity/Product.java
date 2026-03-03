package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.Categories;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private int id;

    @NotBlank(message = "Название продукта обязательно")
    @Size(min = 2, max = 100, message = "Количество символов от 2 до 100")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Категория обязательна")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categories category;

    @NotBlank(message = "Штрихкод обязателен")
    @Column(nullable = false, unique = true)
    private String barcode;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductInstance> instances = new ArrayList<>();

    @Column(length = 100)
    private String brand;

    private double calories;
    private double proteins;
    private double fats;
    private double carbohydrates;
}
