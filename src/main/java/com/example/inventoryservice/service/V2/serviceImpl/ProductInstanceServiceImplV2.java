package com.example.inventoryservice.service.V2.serviceImpl;


import com.example.inventoryservice.dto.TransferProductDto;
import com.example.inventoryservice.dto.ProductInstanceDto;
import com.example.inventoryservice.dto.mapping.ProductInstanceMapper;
import com.example.inventoryservice.dto.mapping.TransferProductMapper;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import com.example.inventoryservice.exceptions.ExistsException;
import com.example.inventoryservice.exceptions.MissingException;
import com.example.inventoryservice.feignclient.UserClient;
import com.example.inventoryservice.repository.ProductInstanceRepository;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.V2.ProductInstanceServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductInstanceServiceImplV2 implements ProductInstanceServiceV2 {

    private final ProductInstanceMapper productInstanceMapper;
    private final TransferProductMapper transferProductMapper;

    private final ProductInstanceRepository productInstanceRepository;
    private final ProductRepository productRepository;
    private final UserClient userClient;

    @Override
    public List<ProductInstanceDto> findInstancesForUser(Integer userId) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не существует");
        }

        List<ProductInstance> instances = productInstanceRepository.findByUserId(userId);
        List<ProductInstanceDto> instanceDtos;
        instanceDtos = instances.stream().map(productInstanceMapper::toDto).toList();

        return instanceDtos;
    }

    @Override
    public ProductInstanceDto findProductInstanceById(int id, Integer userId) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не существует");
        }

        ProductInstance productInstance = productInstanceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new MissingException("Экземпляр продукта id '" + id + "' не найден у пользователя " + userId));

        return productInstanceMapper.toDto(productInstance);
    }

    @Transactional
    @Override
    public ProductInstanceDto createProductInstanceForUser(ProductInstanceDto productInstanceDto, Integer userId) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не существует");
        }

        Product product = productRepository.findById(productInstanceDto.getProductId())
                .orElseThrow(() -> new MissingException("Продукт с id " + productInstanceDto.getProductId() + " не найден"));

        if (product.getOwnerId() != null && !product.getOwnerId().equals(userId)) {
            throw new ExistsException("Нельзя добавить экземпляр продукта id '" + product.getId() + "', который принадлежит другому пользователю или не является глобальным");
        }

        ProductInstance instance = productInstanceMapper.toEntity(productInstanceDto);

        if (instance.getCreatedAt() == null) {
            instance.setCreatedAt(LocalDateTime.now());
        }

        instance.setUserId(userId);
        instance.setProduct(product);
        productInstanceRepository.save(instance);

        return productInstanceMapper.toDto(instance);
    }

    @Transactional
    @Override
    public ProductInstanceDto updateProductInstance(int id, Integer userId, ProductInstanceDto productInstanceDto) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не существует");
        }

        ProductInstance receivedProductInstance = productInstanceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new MissingException("Экземляр продукта id " + id + " не существует"));

        Product existingProduct = findProductForInstance(productInstanceDto);
        productInstanceMapper.updateFromDto(productInstanceDto, receivedProductInstance);
        receivedProductInstance.setProduct(existingProduct);
        receivedProductInstance.setUserId(userId);
        productInstanceRepository.save(receivedProductInstance);

        return productInstanceMapper.toDto(receivedProductInstance);

    }

    @Transactional
    @Override
    public void deleteProductInstanceById(int id, Integer userId) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не существует");
        }

        if(!productInstanceRepository.existsByIdAndUserId(id, userId)) {
            throw new MissingException("Экземпляр продукта id " + id + " не существует");
        }

        productInstanceRepository.deleteById(id);
    }

    private Product findProductForInstance(ProductInstanceDto productInstanceDto) {
        int productId = productInstanceDto.getProductId();
        if (!productRepository.existsById(productId)) {
            throw new MissingException("Для экземляра продукта не верно указан id продукта или вовсе не указан");
        }

        return productRepository.findById(productId).orElseThrow();
    }


    @Override
    public List<TransferProductDto> getExpiring(Integer userId) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не существует");
        }

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(5);

        List<ProductInstance> expiringInstances = productInstanceRepository.findByUserIdAndExpirationDateBetween(userId, today, futureDate);

        if (expiringInstances.isEmpty()) {
            return new ArrayList<>();
        }

        List<TransferProductDto> result = new ArrayList<>();

        for (ProductInstance instance : expiringInstances) {
            Product product = instance.getProduct();

            TransferProductDto existingDto = null;
            for (TransferProductDto dto : result) {
                if (dto.getId() == product.getId()) {
                    existingDto = dto;
                    break;
                }
            }

            if (existingDto == null) {
                TransferProductDto newDto = createTransferProduct(product);
                newDto.addInstance(productInstanceMapper.toDto(instance));
                newDto.setExpirationDate(instance.getExpirationDate());
                result.add(newDto);
            }
            else {
                existingDto.addInstance(productInstanceMapper.toDto(instance));

                if (instance.getExpirationDate().isBefore(existingDto.getExpirationDate())) {
                    existingDto.setExpirationDate(instance.getExpirationDate());
                }
            }
        }

        return result;
    }

    private TransferProductDto createTransferProduct(Product product) {
        TransferProductDto transferProductDto = new TransferProductDto();
        transferProductMapper.updateFromEntity(product, transferProductDto);

        return transferProductDto;
    }
}
