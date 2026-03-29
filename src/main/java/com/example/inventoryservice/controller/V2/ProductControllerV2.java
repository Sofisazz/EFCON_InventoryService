package com.example.inventoryservice.controller.V2;

import com.example.inventoryservice.dto.ConsumeProductDto;
import com.example.inventoryservice.dto.ProductAvailabilityDto;
import com.example.inventoryservice.dto.ProductDto;
import com.example.inventoryservice.dto.ProductStatusDto;
import com.example.inventoryservice.service.V2.ProductServiceV2;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/products")
public class ProductControllerV2 {

    private final ProductServiceV2 productService;

    @GetMapping()
    List<ProductDto> findAllProducts(@RequestParam Integer userId){
        return productService.findAllProducts(userId);
    }

    @GetMapping("/{name}")
    List<ProductDto> findProductsByName(@PathVariable String name,
                                        @RequestParam Integer userId) {
        return productService.findProductsByName(name, userId);
    }

    @PostMapping()
    public ProductDto createProduct(@Valid @RequestBody ProductDto productDto,
                                    @RequestParam(required = false) Integer userId) {
        return productService.createProduct(productDto, userId);
    }

    @PutMapping("/{id}")
    public ProductDto changeProduct(@PathVariable int id,
                                    @Valid @RequestBody ProductDto productDto,
                                    @RequestParam(required = false) Integer userId) {
        return productService.updateProduct(id, productDto, userId);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Integer id,
                              @RequestParam(required = false) Integer userId)  {
        productService.deleteProductById(id, userId);
    }

    @PostMapping("/scan/{barcode}")
    public ProductDto findOrCreateByBarcode(@PathVariable String barcode,
                                            @RequestParam(required = false) Integer userId) {
        return productService.findOrCreateByBarcode(barcode, userId);
    }

    @PostMapping("/check")
    public List<ProductAvailabilityDto> checkAvailability(@RequestParam Integer userId,
                                                          @RequestBody List<String> productNames) {
        return productService.checkProductsAvailability(userId, productNames);
    }

    @PostMapping("/shopping-list")
    public List<ProductStatusDto> generateShoppingList(@RequestParam Integer userId,
                                                       @RequestBody List<String> productNames) {
        return productService.generateShoppingList(userId, productNames);
    }

    @PostMapping("/consume")
    public void consumeProduct(@RequestBody ConsumeProductDto dto) {
        productService.consumeProduct(dto.getUserId(), dto.getProductName(), dto.getAmount(), dto.getUnit());
    }

    @PostMapping("/return")
    public void returnProduct(@RequestBody ConsumeProductDto dto) {
        productService.returnProduct(dto.getUserId(), dto.getProductName(), dto.getAmount(), dto.getUnit());
    }
}
