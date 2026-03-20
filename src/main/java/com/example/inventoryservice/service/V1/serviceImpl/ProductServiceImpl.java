package com.example.inventoryservice.service.V1.serviceImpl;

import com.example.inventoryservice.dto.*;
import com.example.inventoryservice.dto.mapping.ProductInstanceMapper;
import com.example.inventoryservice.dto.mapping.ProductMapper;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import com.example.inventoryservice.exceptions.ExistsException;
import com.example.inventoryservice.exceptions.MissingException;
import com.example.inventoryservice.exceptions.UpdateProductException;
import com.example.inventoryservice.map.CategoryMap;
import com.example.inventoryservice.repository.ProductInstanceRepository;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.V1.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductInstanceRepository productInstanceRepository;
    private final ProductMapper productMapper;
    private final ProductInstanceMapper productInstanceMapper;
    private final RestClient restClient;
    private final CategoryMap categoryMap;

    @Value("${openfoodfacts.url}")
    private String url;

    @Override
    public Page<ProductDto> findAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toDto);
    }


    @Override
    public ProductDto findProductById(int id) {
        return productRepository.findById(id).map(productMapper::toDto)
                .orElseThrow(() -> new MissingException("Продукта id " + id + " не существует"));
    }

    @Transactional
    @Override
    public ProductDto createProduct(ProductDto productDto) {
        if (productRepository.existsByBarcode(productDto.getBarcode())) {
            throw new ExistsException("Продукт со штрихкодом '" + productDto.getBarcode() + "' существует");
        }

        Product product = productMapper.toEntity(productDto);
        productRepository.save(product);

        return productMapper.toDto(product);
    }

    @Transactional
    @Override
    public ProductDto updateProduct(int id, ProductDto productDto) {
        Product receivedProduct = productRepository.findById(id)
                .orElseThrow(() -> new MissingException("Продукт id " + id + " не существует"));

        existProductByBarcode(productDto.getBarcode(), id);
        productMapper.updateFromDto(productDto, receivedProduct);
        productRepository.flush();

        return productMapper.toDto(productRepository.save(receivedProduct));
    }

    @Transactional
    @Override
    public void deleteProductById(int id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
        } else throw new MissingException("Продукт id " + id + " не существует");
    }

    @Override
    public List<ProductInstanceDto> findAllInstancesForProductById(int idPr) {
        return productInstanceRepository.findByProductId(idPr).stream().map(productInstanceMapper::toDto).toList();
    }

    @Override
    public ProductInstanceDto findInstanceForProductById(int idPr, int idInst) {
        return productInstanceRepository.findByIdAndProductId(idInst, idPr).map(productInstanceMapper::toDto)
                .orElseThrow(() -> new MissingException("Экземпляр продукта с id " + idInst + " не найден для продукта с id " + idPr));
    }

    @Transactional
    @Override
    public ProductInstanceDto createInstanceForProductById(int idPr, ProductInstanceDto productInstanceDto) {
        Product product = productRepository.findById(idPr)
                .orElseThrow(() -> new MissingException("Продукт id " + idPr + " не существует"));

        ProductInstance newProductInstance = productInstanceMapper.toEntity(productInstanceDto);
        newProductInstance.setProduct(product);
        productInstanceRepository.save(newProductInstance);

        return productInstanceMapper.toDto(newProductInstance);
    }

    @Transactional
    @Override
    public void deleteInstanceForProductById(int idPr, int idInst) {
        if (productInstanceRepository.existsByIdAndProductId(idInst, idPr)) {
            productInstanceRepository.deleteById(idInst);
        } else throw new MissingException("Экземпляр продукта с id " + idInst + " не найден для продукта с id " + idPr);
    }

    @Override
    public ProductDto findOrCreateByBarcode(String barcode) {
        if (productRepository.existsByBarcode(barcode)) {
            return productMapper.toDto(productRepository.findByBarcode(barcode).orElseThrow());
        }

        OpenFoodDto response = null;

        try {
            response = restClient.get().uri(url + "{barcode}.json", barcode).retrieve().body(OpenFoodDto.class);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }

        Product newProduct = addProductFromApi(response, barcode);

        return productMapper.toDto(productRepository.save(newProduct));
    }

    private void existProductByBarcode(String barcode, int id) {
        if (productRepository.existsByBarcode(barcode)) {
            Product existingProductWithBarcode = productRepository.findByBarcode(barcode).orElseThrow();

            if (existingProductWithBarcode.getId() != id) {
                throw new UpdateProductException("Продукт со штрихкодом '" + barcode + "' существует");
            }
        }
    }

    private Product addProductFromApi(OpenFoodDto response, String barcode) {
        ProductDataDto data = Optional.ofNullable(response)
                .map(OpenFoodDto::getProduct)
                .orElseThrow(() -> new MissingException("Ответ не найден в глобальной базе"));

        String receivedName = Optional.ofNullable(data.getProductNameRu())
                .filter(nameRu -> !nameRu.isBlank())
                .or(() -> Optional.ofNullable(data.getProductName())
                        .filter(productName -> !productName.isBlank()))
                .or(() -> Optional.ofNullable(data.getGenericName())
                        .filter(genericName -> !genericName.isBlank()))
                .orElseThrow(() -> new MissingException("Продукт со штрихкодом " + barcode + " не найден в глобальной базе."));

        Product newProduct = new Product();
        newProduct.setBarcode(barcode);
        newProduct.setName(decodeHtmlEntities(receivedName));
        newProduct.setCategory(categoryMap.defineCategory(receivedName));
        newProduct.setBrand(decodeHtmlEntities(data.getBrands()));

        NutrimentsDto n = Optional.ofNullable(data.getNutriments()).orElseThrow();
        newProduct.setCalories(n.getEnergyKcal());
        newProduct.setProteins(n.getProteins());
        newProduct.setFats(n.getFat());
        newProduct.setCarbohydrates(n.getCarbohydrates());

        return newProduct;
    }

    private String decodeHtmlEntities(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("&quot;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
