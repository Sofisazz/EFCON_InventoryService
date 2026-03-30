package com.example.inventoryservice.service.V2;

import com.example.inventoryservice.dto.ConsumeProductDto;

@SuppressWarnings("unused")
public interface KafkaConsumerService {
    void consumeEatenProducts(ConsumeProductDto consumeProduct);
    void returnProducts(ConsumeProductDto consumeProduct);
}
