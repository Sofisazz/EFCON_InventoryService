package com.example.inventoryservice.service.V1.serviceImpl;

import com.example.inventoryservice.dto.ProductInstanceDto;
import com.example.inventoryservice.dto.mapping.ProductInstanceMapper;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import com.example.inventoryservice.exceptions.MissingException;
import com.example.inventoryservice.repository.ProductInstanceRepository;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.V1.ProductInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductInstanceServiceImpl implements ProductInstanceService {

    private final ProductInstanceMapper productInstanceMapper;
    private final ProductInstanceRepository productInstanceRepository;
    private final ProductRepository productRepository;

    @Override
    public List<ProductInstanceDto> findAllProductInstances() {
        return productInstanceRepository.findAll().stream().map(productInstanceMapper::toDto).toList();
    }

    @Override
    public ProductInstanceDto findProductInstanceById(int id) {
        return productInstanceRepository.findById(id).map(productInstanceMapper::toDto)
                .orElseThrow(() -> new MissingException("Экземляр продукта id " + id + " не существует"));
    }

    @Transactional
    @Override
    public ProductInstanceDto createProductInstance(ProductInstanceDto productInstanceDto) {
        Product existingProduct = findProductForInstance(productInstanceDto);

        ProductInstance receivedProductInstance = productInstanceMapper.toEntity(productInstanceDto);
        receivedProductInstance.setProduct(existingProduct);

        return productInstanceMapper.toDto(productInstanceRepository.save(receivedProductInstance));
    }

    @Transactional
    @Override
    public ProductInstanceDto updateProductInstance(int id, ProductInstanceDto productInstanceDto) {
        ProductInstance receivedProductInstance = productInstanceRepository.findById(id)
                .orElseThrow(() -> new MissingException("Экземляр продукт id " + id + " не существует"));

        Product existingProduct = findProductForInstance(productInstanceDto);
        productInstanceMapper.updateFromDto(productInstanceDto, receivedProductInstance);
        receivedProductInstance.setProduct(existingProduct);

        return productInstanceMapper.toDto(productInstanceRepository.save(receivedProductInstance));
    }

    @Transactional
    @Override
    public void deleteProductInstanceById(int id)  {
        if(productInstanceRepository.existsById(id)) {
            productInstanceRepository.deleteById(id);
        } else throw new MissingException("Экземпляр продукта id " + id + " не существует");
    }

    private Product findProductForInstance(ProductInstanceDto productInstanceDto) {
        int productId = productInstanceDto.getProductId();
        if (!productRepository.existsById(productId)) {
            throw new MissingException("Для экземляра продукта не верно указан id продукта или вовсе не указан");
        }

        return productRepository.findById(productId).orElseThrow();
    }
}
