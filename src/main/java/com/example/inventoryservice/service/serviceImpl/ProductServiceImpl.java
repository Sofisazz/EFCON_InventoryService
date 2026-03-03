package com.example.inventoryservice.service.serviceImpl;

import com.example.inventoryservice.dto.*;
import com.example.inventoryservice.dto.mapping.ProductInstanceMapper;
import com.example.inventoryservice.dto.mapping.ProductMapper;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.entity.ProductInstance;
import com.example.inventoryservice.enums.Categories;
import com.example.inventoryservice.exceptions.ExistsException;
import com.example.inventoryservice.exceptions.MissingException;
import com.example.inventoryservice.exceptions.UpdateProductException;
import com.example.inventoryservice.repository.ProductInstanceRepository;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductInstanceRepository productInstanceRepository;
    private final ProductMapper productMapper;
    private final ProductInstanceMapper productInstanceMapper;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public List<ProductDto> findAllProducts() {
        return productRepository.findAll().stream().map(productMapper::toDto).toList();
    }


    @Override
    public ProductDto findProductById(int id) {
        return productRepository.findById(id).map(productMapper::toDto)
                .orElseThrow(() -> new MissingException("Продукта id " + id + " не существует"));
    }


    @Override
    public ProductDto createProduct(ProductDto productDto) {
        if(productRepository.existsByBarcode(productDto.getBarcode())) {
            throw new ExistsException("Продукт со штрихкодом '" + productDto.getBarcode() + "' существует");
        }

        Product product = productMapper.toEntity(productDto);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    public ProductDto updateProduct(int id,ProductDto productDto) {
        Product receivedProduct = productRepository.findById(id)
                .orElseThrow(() -> new MissingException("Продукт id " + id + " не существует"));

        existProductByBarcode(productDto.getBarcode(), id);
        Product newProduct = refreshProduct(receivedProduct, productDto);

        return productMapper.toDto(productRepository.save(newProduct));
    }

    @Override
    public void deleteProductById(int id) {
        if(productRepository.existsById(id)) {
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

    @Override
    public ProductInstanceDto createInstanceForProductById(int idPr, ProductInstanceDto productInstanceDto) {
        Product product = productRepository.findById(idPr)
                .orElseThrow(() -> new MissingException("Продукт id " + idPr + " не существует"));

        ProductInstance newProductInstance = productInstanceMapper.toEntity(productInstanceDto);
        newProductInstance.setProduct(product);
        productInstanceRepository.save(newProductInstance);

        return productInstanceMapper.toDto(newProductInstance);
    }

    @Override
    public void deleteInstanceForProductById(int idPr, int idInst) {
        if(productInstanceRepository.existsByIdAndProductId(idInst, idPr)) {
            productInstanceRepository.deleteById(idInst);
        } else throw new MissingException("Экземпляр продукта с id " + idInst + " не найден для продукта с id " + idPr);
    }

    @Override
    public ProductDto findOrCreateByBarcode(String barcode) {
        if(productRepository.existsByBarcode(barcode)) {
            return productMapper.toDto(productRepository.findByBarcode(barcode).orElseThrow());
        }

        OpenFoodDto response = null;
        try {
            response = webClient.get()
                    .uri("https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json")
                    .retrieve()
                    .bodyToMono(OpenFoodDto.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (Exception e) {
            System.err.println("Ошибка сети: " + e.getMessage());
        }

        Product newProduct = addProductFromApi(response, barcode);

        return productMapper.toDto(productRepository.save(newProduct));
    }

    private void existProductByBarcode(String barcode, int id) {
        if(productRepository.existsByBarcode(barcode)) {
            Product existingProductWithBarcode = productRepository.findByBarcode(barcode).orElseThrow();

            if(existingProductWithBarcode.getId() != id) {
                throw new UpdateProductException("Продукт со штрихкодом '" + barcode + "' существует");
            }
        }
    }

    private Product refreshProduct(Product product, ProductDto productDto){
        product.setName(productDto.getName());
        product.setCategory(productDto.getCategory());
        product.setBarcode(productDto.getBarcode());

        return product;
    }

    private Product addProductFromApi(OpenFoodDto response, String barcode) {
        String name = null;
        ProductDataDto data = null;

        if (response != null && response.getProduct() != null) {
            data = response.getProduct();

            if (data.getProductNameRu() != null && !data.getProductNameRu().isEmpty()) {
                name = data.getProductNameRu();
            } else if (data.getProductName() != null && !data.getProductName().isEmpty()) {
                name = data.getProductName();
            } else if (data.getGenericName() != null && !data.getGenericName().isEmpty()) {
                name = data.getGenericName();
            }
        }

        if (name == null || name.isEmpty()) {
            throw new MissingException("Продукт со штрихкодом " + barcode + " не найден в глобальной базе.");
        }

        Product newProduct = new Product();
        newProduct.setBarcode(barcode);
        newProduct.setName(decodeHtmlEntities(name));
        newProduct.setCategory(findCategoriesForProduct(name));

        if (data != null) {
            newProduct.setBrand(decodeHtmlEntities(data.getBrands()));

            if (data.getNutriments() != null) {
                NutrimentsDto n = data.getNutriments();
                newProduct.setCalories(n.getEnergyKcal());
                newProduct.setProteins(n.getProteins());
                newProduct.setFats(n.getFat());
                newProduct.setCarbohydrates(n.getCarbohydrates());
            }
        }

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

    private Categories findCategoriesForProduct(String receivedName){
        Categories category = Categories.OTHER;
        String lower = receivedName.toLowerCase();

        if (lower.contains("молок") || lower.contains("сыр") || lower.contains("йогурт") ||
                lower.contains("кефир") || lower.contains("творог") || lower.contains("сметан") ||
                lower.contains("сливок") || lower.contains("масло") || lower.contains("яиц") ||
                lower.contains("яичн") || lower.contains("ряженк") || lower.contains("простокваш")) {
            category = Categories.DAIRY_AND_EGGS;

        } else if (lower.contains("говядин") || lower.contains("свинин") || lower.contains("баранин") ||
                lower.contains("телятин") || lower.contains("фарш") || lower.contains("стейк") ||
                lower.contains("ребрышк") || lower.contains("грудинк") || lower.contains("вырезк")) {
            category = Categories.MEAT;

        } else if (lower.contains("колбас") || lower.contains("сосиск") || lower.contains("сарделек") ||
                lower.contains("ветчин") || lower.contains("карбонат") || lower.contains("бекон") ||
                lower.contains("паштет") || lower.contains("деликатес") || lower.contains("копченост")) {
            category = Categories.PROCESSED_MEAT;

        } else if (lower.contains("рыб") || lower.contains("тунец") || lower.contains("кревет") ||
                lower.contains("миди") || lower.contains("устриц") || lower.contains("кальмар") ||
                lower.contains("осьминог") || lower.contains("краб") || lower.contains("икр") ||
                lower.contains("сельд") || lower.contains("лосос") || lower.contains("горбуш") ||
                lower.contains("минтай") || lower.contains("треск") || lower.contains("скумбр")) {
            category = Categories.SEAFOOD;

        } else if (lower.contains("овощ") || lower.contains("фрукт") || lower.contains("ягод") ||
                lower.contains("картоф") || lower.contains("капуст") || lower.contains("морков") ||
                lower.contains("лук") || lower.contains("чеснок") || lower.contains("помидор") ||
                lower.contains("огурец") || lower.contains("яблок") || lower.contains("банан") ||
                lower.contains("апельсин") || lower.contains("лимон") || lower.contains("зелень") ||
                lower.contains("укроп") || lower.contains("петрушк") || lower.contains("салат") ||
                lower.contains("гриб")) {
            category = Categories.FRESH_PRODUCE;

        } else if (lower.contains("заморож") || lower.contains("пельмен") || lower.contains("вареник") ||
                lower.contains("блинчик") || lower.contains(" морожен") || lower.contains("смесь") ||
                lower.contains("полуфабрикат")) {
            category = Categories.FROZEN_FOODS;

        } else if (lower.contains("консерв") || lower.contains("тушенк") || lower.contains("кукуруз") ||
                lower.contains("горошек") || lower.contains("фасоль") ||
                lower.contains("шпрот") || lower.contains("сайра")) {
            category = Categories.CANNED_GOODS;

        } else if (lower.contains("макарон") || lower.contains("круп") || lower.contains("рис") ||
                lower.contains("гречк") || lower.contains("овсян") || lower.contains("пшен") ||
                lower.contains("перловк") || lower.contains("манн") || lower.contains("спагетт") ||
                lower.contains("лапш") || lower.contains("вермишел") || lower.contains("хлопья")) {
            category = Categories.GRAINS_AND_PASTA;

        } else if (lower.contains("шоколад") || lower.contains("конфет") || lower.contains("печен") ||
                lower.contains("торт") || lower.contains("пирожн") || lower.contains("вафл") ||
                lower.contains("пряник") || lower.contains("мармелад") || lower.contains("зефир") ||
                lower.contains("пастил") || lower.contains("сахар") || lower.contains("мед") ||
                lower.contains("варень") || lower.contains("джем") || lower.contains("мюсли") ||
                lower.contains("батончик")) {
            category = Categories.SWEETS;

        } else if (lower.contains("хлеб") || lower.contains("булк") || lower.contains("сдоб") ||
                lower.contains("батон") || lower.contains("багет") || lower.contains("лаваш") ||
                lower.contains("пирожк") || lower.contains("круассан") || lower.contains("сухар") ||
                lower.contains("тост") || lower.contains("лепешк")) {
            category = Categories.BAKERY;

        } else if (lower.contains("соус") || lower.contains("кетчуп") || lower.contains("майонез") ||
                lower.contains("маринад") || lower.contains("паста") || lower.contains("томат") ||
                lower.contains("горчиц") || lower.contains("хрен") || lower.contains("уксус") ||
                lower.contains("приправ") || lower.contains("специ") || lower.contains("масло растит") ||
                lower.contains("укроп суш") || lower.contains("базилик")) {
            category = Categories.CONDIMENTS;

        } else if (lower.contains("чипс") || lower.contains("сухарик") || lower.contains("снек") ||
                lower.contains("попкорн") || lower.contains("орех") || lower.contains("семечк") ||
                lower.contains("сушен") || lower.contains("вялен") || lower.contains("фисташк") ||
                lower.contains("миндаль") || lower.contains("арахис")) {
            category = Categories.SNACKS;

        } else if (lower.contains("детск питан") || lower.contains("пюре детск") || lower.contains("смесь детск") ||
                lower.contains("каш детск") || lower.contains("для детей") || lower.contains("для младенц")) {
            category = Categories.BABY_FOOD;

        } else if (lower.contains("пиво") || lower.contains("вин") || lower.contains("коньяк") ||
                lower.contains("ликер") || lower.contains("шампан") || lower.contains("водк") ||
                lower.contains("виски") || lower.contains("ром") || lower.contains("джин") ||
                lower.contains("сидр") || lower.contains("медовух")) {
            category = Categories.ALCOHOL;

        } else if (lower.contains("сок") || lower.contains("вода") || lower.contains("чай") ||
                lower.contains("кофе") || lower.contains("лимонад") || lower.contains("газиров") ||
                lower.contains("энергетик") || lower.contains("квас") || lower.contains("морс") ||
                lower.contains("компот") || lower.contains("коктейль") || lower.contains("какао") || lower.contains("минерал") ||
                lower.contains("cola") || lower.contains("sprit") || lower.contains("up") || lower.contains("mirid")) {
            category = Categories.NON_ALCOHOLIC_DRINKS;

        } else if (lower.contains("готов") || lower.contains("блюдо") || lower.contains("пицца") ||
                lower.contains("суп") || lower.contains("борщ") || lower.contains("плов") ||
                lower.contains("гарнир") || lower.contains("салат готов") || lower.contains("шаурм")) {
            category = Categories.READY_MEALS;
        }

        return category;
    }
}
