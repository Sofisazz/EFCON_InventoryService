package com.example.inventoryservice.service.V2.serviceImpl;

import com.example.inventoryservice.dto.*;
import com.example.inventoryservice.dto.mapping.ProductMapper;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import com.example.inventoryservice.enums.Measure;
import com.example.inventoryservice.exceptions.ExistsException;
import com.example.inventoryservice.exceptions.MissingException;
import com.example.inventoryservice.exceptions.UpdateProductException;
import com.example.inventoryservice.feignclient.UserClient;
import com.example.inventoryservice.map.CategoryMap;
import com.example.inventoryservice.repository.ProductInstanceRepository;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.V2.ProductServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductServiceImplV2 implements ProductServiceV2 {

    private final ProductRepository productRepository;
    private final ProductInstanceRepository productInstanceRepository;

    private final ProductMapper productMapper;

    private final CategoryMap categoryMap;

    private final RestClient restClient;
    private final UserClient userClient;

    @Value("${openfoodfacts.url}")
    private String url;



    @Override
    public List<ProductDto> findAllProducts(Integer ownerId) {
        List<Product> products = productRepository.findByOwnerIdIsNullOrOwnerId(ownerId);
        List<ProductDto> productDtos;
        productDtos = products.stream().map(productMapper::toDto).toList();

        return productDtos;
    }

    @Override
    public List<ProductDto> findProductsByName(String name, Integer userId) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не найден");
        }

        List<Product> globalProducts = productRepository.findByNameContainingIgnoreCaseAndOwnerIdIsNull(name);
        List<Product> userProducts = productRepository.findByNameContainingIgnoreCaseAndOwnerId(name, userId);

        Set<Product> productSet= new HashSet<>();

        productSet.addAll(globalProducts);
        productSet.addAll(userProducts);

        List<ProductDto> productDtos;
        productDtos = productSet.stream().map(productMapper::toDto).toList();

        return productDtos;
    }

    @Transactional
    @Override
    public ProductDto createProduct(ProductDto productDto, Integer userId) {
        if(userId != null && !userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не найден");
        }

        String barcode = productDto.getBarcode();
        if (productRepository.existsByBarcodeAndOwnerIdIsNull(barcode)){
            throw new ExistsException("Продукт со штрихкодом '" + barcode + "' уже существует в глобальной базе");
        }

        if (productRepository.existsByBarcodeAndOwnerId(barcode, userId)) {
            throw new ExistsException("Продукт со штрихкодом '" + barcode + "' уже существует в базе пользователя с id '" + userId + "'");
        }

        Product product = productMapper.toEntity(productDto);
        product.setOwnerId(userId);
        productRepository.save(product);

        return productMapper.toDto(product);
    }

    @Transactional
    @Override
    public ProductDto updateProduct(int id, ProductDto productDto, Integer userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new MissingException("Продукт с id " + id + " не найден"));

        if (userId == null) {
            if (product.getOwnerId() != null) {
                throw new ExistsException("Разработчик может изменять только глобальные продукты");
            }
        } else {
            if (!userClient.checkUserExists(userId)) {
                throw new MissingException("Пользователь с id '" + userId + "' не найден");
            }

            if (product.getOwnerId() == null) {
                throw new ExistsException("Пользователи не могут изменять глобальные продукты");
            }

            if (!product.getOwnerId().equals(userId)) {
                throw new ExistsException("Вы не можете изменить этот продукт, так как он принадлежит другому пользователю");
            }
        }

            existProductByBarcode(productDto.getBarcode(), id, userId);
            productMapper.updateFromDto(productDto, product);
            productRepository.flush();

            return productMapper.toDto(product);
    }

    @Override
    public void consumeProduct(int userId, String productName, double amount, Measure recipeUnit) {
        List<Product> products = productRepository.findByNameIgnoreCaseAndOwnerIdIsNull(productName);
        List<Product> productsUser = productRepository.findByNameIgnoreCaseAndOwnerId(productName, userId);
        products.addAll(productsUser);

        if (products.isEmpty()) {
            throw new MissingException("Продукт '" + productName + "' не найден");
        }

        Product product = products.get(0);

        LocalDate today = LocalDate.now();
        List<ProductInstance> instances = productInstanceRepository.findByProductIdAndUserIdAndExpirationDateAfter(product.getId(), userId, today.minusDays(1))
                .stream()
                .sorted(Comparator.comparing(ProductInstance::getExpirationDate)).toList();

        if (instances.isEmpty()) {
            throw new MissingException("Нет доступных экземпляров продукта '" + productName + "' (возможно, истек срок годности)");
        }


        double remainingToConsume = amount;
        List<Integer> idsToDelete = new ArrayList<>();

        for (ProductInstance instance : instances) {
            if (remainingToConsume <= 0.001) {
                break;
            }

            Measure instanceUnit = instance.getUnit();
            double currentCount = instance.getCount();

            double neededInInstanceUnit = convertAmount(remainingToConsume, recipeUnit, instanceUnit);

            if (currentCount >= neededInInstanceUnit) {
                double newCount = currentCount - neededInInstanceUnit;

                if (newCount <= 0.001) {
                    idsToDelete.add(instance.getId());
                } else {
                    instance.setCount(newCount);
                }

                remainingToConsume = 0;

            } else {
                remainingToConsume -= convertAmount(currentCount, instanceUnit, recipeUnit);
                idsToDelete.add(instance.getId());
            }
        }

        if (remainingToConsume > 0.001) {
            throw new MissingException("Недостаточно продукта '" + productName + "'. Требовалось: " + amount + ", доступно: " + (amount - remainingToConsume));
        }

        List<ProductInstance> toSave = new ArrayList<>();

        for (ProductInstance instance : instances) {

            if (!idsToDelete.contains(instance.getId())) {
                toSave.add(instance);
            }
        }

        if (!toSave.isEmpty()) {
            productInstanceRepository.saveAll(toSave);
        }

        if (!idsToDelete.isEmpty()) {
            productInstanceRepository.deleteAllById(idsToDelete);
        }
    }

    @Override
    public void returnProduct(int userId, String productName, double amount, Measure unit) {
        List<Product> products = productRepository.findByNameIgnoreCaseAndOwnerIdIsNull(productName);
        List<Product> productsUser = productRepository.findByNameIgnoreCaseAndOwnerId(productName, userId);
        products.addAll(productsUser);

        if (products.isEmpty()) {
            throw new MissingException("Невозможно вернуть продукт '" + productName + "', так как он не найден");
        }

        Product product = products.get(0);
        LocalDate today = LocalDate.now();

        List<ProductInstance> activeInstances = productInstanceRepository.findByProductIdAndUserIdAndExpirationDateAfter(product.getId(), userId, today.minusDays(1)).stream()
                .sorted(Comparator.comparing(ProductInstance::getExpirationDate)).toList();

        if (!activeInstances.isEmpty()) {
            ProductInstance targetInstance = activeInstances.get(0);

            double amountToAdd = convertAmount(amount, unit, targetInstance.getUnit());

            targetInstance.setCount(targetInstance.getCount() + amountToAdd);
            productInstanceRepository.save(targetInstance);

            } else {
            ProductInstance newInstance = createProductInstance(product, userId, amount, unit, today);

            productInstanceRepository.save(newInstance);
        }
    }

    @Transactional
    @Override
    public void deleteProductById(Integer id, Integer userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new MissingException("Продукт с id " + id + " не найден"));

        if (userId == null) {
            if (product.getOwnerId() != null) {
                throw new ExistsException("Разработчик может удалять только продукты из глобальной базы");
            }
            productRepository.delete(product);
            return;
        }

        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не найден");
        }

        if (product.getOwnerId() == null) {
            throw new ExistsException("Невозможно удалить продукт из глобальной базы. Вы можете удалить только свои личные продукты");
        }

        if (!product.getOwnerId().equals(userId)) {
            throw new ExistsException("Вы не можете удалить этот продукт, так как он принадлежит другому пользователю");
        }

        productRepository.delete(product);
    }

    @Transactional
    @Override
    public ProductDto findOrCreateByBarcode(String barcode, Integer userId) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не найден");
        }

        Optional<Product> existingGlobal = productRepository.findByBarcodeAndOwnerIdIsNull(barcode);
        if (existingGlobal.isPresent()) {
            return productMapper.toDto(existingGlobal.get());
        }

        Optional<Product> existingUserPrivate = productRepository.findByBarcodeAndOwnerId(barcode, userId);
        if (existingUserPrivate.isPresent()) {
            return productMapper.toDto(existingUserPrivate.get());
        }

        OpenFoodDto response = null;

        try {
            response = restClient.get().uri(url + "{barcode}.json", barcode).retrieve().body(OpenFoodDto.class);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }

        Optional.ofNullable(response)
                .orElseThrow(() -> new MissingException("Продукт со штрихкодом " + barcode + " не найден в глобальной базе OpenFoodFacts. Добавьте его вручную"));

        Product newProduct = addProductFromApi(response, barcode, userId);

        return productMapper.toDto(productRepository.save(newProduct));
    }

    @Override
    public List<ProductAvailabilityDto> checkProductsAvailability(Integer userId, List<String> productNames) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не найден");
        }

        List<String> checkProductNames= Optional.ofNullable(productNames).orElse(List.of());

        LocalDate today = LocalDate.now().minusDays(1);
        List<ProductAvailabilityDto> result = new ArrayList<>();

        for (String name : checkProductNames) {
            List<Product> products = productRepository.findByNameIgnoreCaseAndOwnerIdIsNull(name);

            products.addAll(productRepository.findByNameIgnoreCaseAndOwnerId(name, userId));

            if (!products.isEmpty()) {
                Product product = products.get(0);
                List<ProductInstance> instances = productInstanceRepository.findByProductIdAndUserIdAndExpirationDateAfter(product.getId(), userId, today);

                double totalBaseAmount = 0;
                Measure displayUnit = Measure.PCS;
                boolean hasInstances = false;

                if (!instances.isEmpty()) {
                    hasInstances = true;
                    displayUnit = instances.get(0).getUnit();

                    for (ProductInstance instance : instances) {
                        totalBaseAmount += convertToBase(instance.getCount(), instance.getUnit());
                    }
                }

                if (hasInstances && totalBaseAmount > 0.001) {
                    double finalAmount = convertFromBase(totalBaseAmount, displayUnit);

                    result.add(createProductAvailabilityDto(product.getName(), true, "В наличии " + finalAmount + " " + displayUnit));
                } else {
                    result.add(createProductAvailabilityDto(product.getName(), false, "Нет в наличии или истек срок"));
                }
            } else {
                result.add(createProductAvailabilityDto(name, false, "Продукт не найден в глобальной бд"));
            }
        }
        return result;
    }

    @Override
    public List<ProductStatusDto> generateShoppingList(Integer userId, List<String> productNames) {
        if (!userClient.checkUserExists(userId)) {
            throw new MissingException("Пользователь с id '" + userId + "' не найден");
        }

        List<String> checkProductNames = Optional.ofNullable(productNames).orElse(List.of());
        LocalDate today = LocalDate.now().minusDays(1);
        List<ProductStatusDto> result = new ArrayList<>();

        for (String name : checkProductNames) {
            List<Product> products = productRepository.findByNameIgnoreCaseAndOwnerIdIsNull(name);
            products.addAll(productRepository.findByNameIgnoreCaseAndOwnerId(name, userId));

            double totalBaseAmount = 0;
            Measure resultUnit = Measure.PCS;
            boolean isAvailable = false;

            if (!products.isEmpty()) {
                Product product = products.get(0);

                List<ProductInstance> instances = productInstanceRepository.findByProductIdAndUserIdAndExpirationDateAfter(product.getId(), userId, today);

                if (!instances.isEmpty()) {
                    resultUnit = instances.get(0).getUnit();

                    for (ProductInstance instance : instances) {
                        double count = instance.getCount();
                        Measure unit = instance.getUnit();

                        double baseValue = convertToBase(count, unit);
                        totalBaseAmount += baseValue;
                    }

                    if (totalBaseAmount > 0.001) {
                        isAvailable = true;
                    }
                }
            }

            double finalAmount = convertFromBase(totalBaseAmount, resultUnit);

            ProductStatusDto newProductStatusDto = createProductStatusDto(name, isAvailable, finalAmount, resultUnit);
            result.add(newProductStatusDto);
        }
        return result;
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

    private double convertToBase(double value, Measure unit) {
        if (unit == null) {
            return value;
        }

        return switch (unit) {
            case KG, L -> value * 1000.0;
            default -> value;
        };
    }

    private double convertFromBase(double baseValue, Measure targetUnit) {
        if (targetUnit == null) {
            return baseValue;
        }

        return switch (targetUnit) {
            case KG, L -> baseValue / 1000.0;
            default -> baseValue;
        };
    }

    private void existProductByBarcode(String barcode, int id, Integer userId) {
        if (productRepository.existsByBarcodeAndOwnerId(barcode, userId)) {
            Product existingProductWithBarcode = productRepository.findByBarcodeAndOwnerId(barcode, userId).orElseThrow();

            if (existingProductWithBarcode.getId() != id) {
                throw new UpdateProductException("Продукт со штрихкодом '" + barcode + "' существует в базе пользователя с id '" + userId + "'");
            }
        }

        if (productRepository.existsByBarcodeAndOwnerIdIsNull(barcode)) {
            Product existingProductWithBarcode = productRepository.findByBarcodeAndOwnerIdIsNull(barcode).orElseThrow();

            if (existingProductWithBarcode.getId() != id) {
                throw new UpdateProductException("Продукт со штрихкодом '" + barcode + "' существует в глобальной базе");
            }
        }
    }

    private Product addProductFromApi(OpenFoodDto response, String barcode, Integer userId) {
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
        newProduct.setOwnerId(userId);

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

    private ProductStatusDto createProductStatusDto(String name, boolean isAvailable, double count, Measure unit){
        ProductStatusDto product = new ProductStatusDto();
        product.setName(name);
        product.setAvailable(isAvailable);
        product.setAvailableAmount(count);
        product.setUnit(unit);

        return product;
    }

    private ProductAvailabilityDto createProductAvailabilityDto(String name, boolean available, String message) {
        ProductAvailabilityDto product = new ProductAvailabilityDto();
        product.setProductName(name);
        product.setAvailable(available);
        product.setMessage(message);

        return product;
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
}
