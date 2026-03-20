package com.example.inventoryservice.service.V1.serviceImpl;

import com.example.inventoryservice.dto.TransferProductDto;
import com.example.inventoryservice.dto.mapping.TransferProductMapper;
import com.example.inventoryservice.dto.mapping.ProductInstanceMapper;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import com.example.inventoryservice.exceptions.MissingException;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.V1.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, List<TransferProductDto>> kafkaTemplate;
    private final ProductInstanceMapper productInstanceMapper;
    private final TransferProductMapper transferProductMapper;
    private final ProductRepository productRepository;

    @Override
    public List<TransferProductDto> sendMessage() {
        List<Product> products = productRepository.findAll();
        List<TransferProductDto> expiringProductsDto = new ArrayList<>();

        for (Product product : products) {
            List<ProductInstance> instances = product.getInstances();
            TransferProductDto transferProductDto = createKafkaProduct(product);

            for (ProductInstance instance : instances) {
                long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), instance.getExpirationDate());

                if(daysBetween <= 5 && daysBetween >= 0) {
                    transferProductDto.addInstance(productInstanceMapper.toDto(instance));
                }
            }

            if (!transferProductDto.getInstances().isEmpty()) {
                expiringProductsDto.add(transferProductDto);
            }
        }

        if (expiringProductsDto.isEmpty()) {
            throw new MissingException("Нет продуктов с истекающим сроком годности");
        }

        kafkaTemplate.send("expiring_date_products", expiringProductsDto);
        return expiringProductsDto;
    }

    private TransferProductDto createKafkaProduct(Product product) {
        TransferProductDto transferProductDto = new TransferProductDto();
        transferProductMapper.updateFromEntity(product, transferProductDto);

        return transferProductDto;
    }
}
