package com.example.inventoryservice.service.V2.serviceImpl;

import com.example.inventoryservice.dto.ConsumeProductDto;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import com.example.inventoryservice.enums.Measure;
import com.example.inventoryservice.exceptions.MissingException;
import com.example.inventoryservice.repository.ProductInstanceRepository;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.V2.KafkaConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaConsumerServiceImpl implements KafkaConsumerService {

    private final ProductRepository productRepository;
    private final ProductInstanceRepository productInstanceRepository;

    @Transactional
    @KafkaListener(topics = "eaten_products", groupId = "my-group")
    public void consumeEatenProducts(ConsumeProductDto consumeProduct) {
        List<Product> products = productRepository.findByNameIgnoreCaseAndOwnerIdIsNull(consumeProduct.getProductName());
        List<Product> productsUser = productRepository.findByNameIgnoreCaseAndOwnerId(consumeProduct.getProductName(), consumeProduct.getUserId());
        products.addAll(productsUser);

        if (products.isEmpty()) {
            throw new MissingException("Продукт '" + consumeProduct.getProductName() + "' не найден");
        }

        LocalDate today = LocalDate.now();
        List<ProductInstance> allInstances = new ArrayList<>();

        for (Product product : products) {
            List<ProductInstance> instances  = productInstanceRepository.findByProductIdAndUserIdAndExpirationDateAfter(product.getId(), consumeProduct.getUserId(), today.minusDays(1));

            allInstances.addAll(instances);
        }

        if (allInstances.isEmpty()) {
            throw new MissingException("Нет доступных экземпляров продукта '" + consumeProduct.getProductName() + "' (возможно, истек срок годности)");
        }

        allInstances.sort(Comparator.comparing(ProductInstance::getExpirationDate));

        double remainingToConsume = consumeProduct.getAmount();

        for (ProductInstance instance : allInstances) {
            if (remainingToConsume <= 0.001) {
                break;
            }

            Measure instanceUnit = instance.getUnit();
            double currentCount = instance.getCount();

            double neededInInstanceUnit = convertAmount(remainingToConsume, consumeProduct.getUnit(), instanceUnit);

            if (currentCount >= neededInInstanceUnit) {
                double newCount = currentCount - neededInInstanceUnit;

                if (newCount <= 0.001) {
                    productInstanceRepository.delete(instance);
                } else {
                    instance.setCount(newCount);
                }

                remainingToConsume = 0;

            } else {
                remainingToConsume -= convertAmount(currentCount, instanceUnit, consumeProduct.getUnit());
                productInstanceRepository.delete(instance);
            }
        }

        if (remainingToConsume > 0.001) {
            throw new MissingException("Недостаточно продукта '" + consumeProduct.getProductName() + "'. Требовалось: " + consumeProduct.getAmount() + ", доступно: " + (consumeProduct.getAmount() - remainingToConsume));
        }
    }

    private double convertAmount(double value, Measure from, Measure to) {
        if (from.equals(to)) {
            return value;
        }

        double baseValue;
        if (from == Measure.KG) {
            baseValue = value * 1000.0;
        } else if (from == Measure.L) {
            baseValue = value * 1000.0;
        } else {
            baseValue = value;
        }

        return switch (to) {
            case KG, L -> baseValue / 1000.0;
            default -> baseValue;
        };

    }

    @Transactional
    @KafkaListener(topics = "returned_products", groupId = "my-group")
    @SuppressWarnings("unused")
    public void returnProducts(ConsumeProductDto consumeProduct) {
        List<Product> products = productRepository.findByNameIgnoreCaseAndOwnerIdIsNull(consumeProduct.getProductName());
        List<Product> productsUser = productRepository.findByNameIgnoreCaseAndOwnerId(consumeProduct.getProductName(), consumeProduct.getUserId());
        products.addAll(productsUser);

        if (products.isEmpty()) {
            throw new MissingException("Невозможно вернуть продукт '" + consumeProduct.getProductName() + "', так как он не найден");
        }

        Product product = products.get(0);
        LocalDate today = LocalDate.now();

        List<ProductInstance> activeInstances = productInstanceRepository.findByProductIdAndUserIdAndExpirationDateAfter(product.getId(), consumeProduct.getUserId(), today.minusDays(1)).stream()
                .sorted(Comparator.comparing(ProductInstance::getExpirationDate)).toList();

        if (!activeInstances.isEmpty()) {
            ProductInstance targetInstance = activeInstances.get(0);

            double amountToAdd = convertAmount(consumeProduct.getAmount(), consumeProduct.getUnit(), targetInstance.getUnit());

            targetInstance.setCount(targetInstance.getCount() + amountToAdd);
            productInstanceRepository.save(targetInstance);

        } else {
            ProductInstance newInstance = createProductInstance(product, consumeProduct.getUserId(), consumeProduct.getAmount(), consumeProduct.getUnit(), today);

            productInstanceRepository.save(newInstance);
        }
    }

    private ProductInstance createProductInstance(Product product, int userId, double amount, Measure unit, LocalDate today) {
        ProductInstance newInstance = new ProductInstance();
        newInstance.setProduct(product);
        newInstance.setUserId(userId);
        newInstance.setCount(amount);
        newInstance.setUnit(unit);
        newInstance.setExpirationDate(today.plusDays(1));
        newInstance.setCreatedAt(LocalDateTime.now());

        return newInstance;
    }

}

