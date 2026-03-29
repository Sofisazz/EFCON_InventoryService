package com.example.inventoryservice.service.V1;

import com.example.inventoryservice.dto.TransferProductDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface KafkaProducerService {
    List<TransferProductDto> sendMessage();
}
