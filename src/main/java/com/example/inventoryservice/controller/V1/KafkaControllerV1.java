package com.example.inventoryservice.controller.V1;

import com.example.inventoryservice.dto.TransferProductDto;
import com.example.inventoryservice.service.V1.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/messages")
public class KafkaControllerV1 {

    private final KafkaProducerService producerService;

    @PostMapping()
    public List<TransferProductDto> sendMessage() {
        return producerService.sendMessage();
    }
}
