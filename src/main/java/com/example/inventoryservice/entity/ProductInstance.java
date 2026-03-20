package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.Measure;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "product_instances")
public class ProductInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instance_id")
    private int id;

    @NotNull(message = "Количество экземпляра обязательно")
    @Column(nullable = false)
    private double count;

    @NotNull(message = "Единица измерения обязательна")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Measure unit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Срок годности обязателен")
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer userId;
}
