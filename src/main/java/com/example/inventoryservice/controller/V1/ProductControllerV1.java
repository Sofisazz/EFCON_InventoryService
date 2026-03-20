package com.example.inventoryservice.controller.V1;

import com.example.inventoryservice.dto.ProductDto;
import com.example.inventoryservice.dto.ProductInstanceDto;
import com.example.inventoryservice.service.V1.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductControllerV1 {
    private final ProductService productService;

    @GetMapping()
    public Page<ProductDto> getAllProducts(@RequestParam(value = "offset", defaultValue = "0") @Min(0) Integer offset,
                                           @RequestParam(value = "limit", defaultValue = "2") @Min(1) @Max(100) Integer limit,
                                           @RequestParam(value = "sortName", defaultValue = "name") String  sortName) {
        return productService.findAllProducts(PageRequest.of(offset, limit, Sort.by(sortName)));
    }

    @GetMapping("/{id}")
    public ProductDto getProduct(@PathVariable int id) {
        return productService.findProductById(id);
    }

    @PostMapping()
    public ProductDto createProduct(@Valid @RequestBody ProductDto productDto) {
        return productService.createProduct(productDto);
    }

    @PutMapping("/{id}")
    public ProductDto changeProduct(@PathVariable int id, @Valid @RequestBody ProductDto productDto) {
        return productService.updateProduct(id, productDto);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable int id)  {
        productService.deleteProductById(id);
    }

    @GetMapping("/{idPr}/instances")
    public List<ProductInstanceDto> getAllInstancesForProduct(@PathVariable int idPr) {
        return productService.findAllInstancesForProductById(idPr);
    }

    @GetMapping("/{idPr}/instances/{idInst}")
    public ProductInstanceDto getInstanceForProduct(@PathVariable int idPr, @PathVariable int idInst) {
        return productService.findInstanceForProductById(idPr, idInst);
    }

    @PostMapping("/{idPr}/instances")
    public ProductInstanceDto createInstanceForProduct(@PathVariable int idPr, @Valid @RequestBody ProductInstanceDto productInstanceDto) {
        return productService.createInstanceForProductById(idPr, productInstanceDto);
    }

    @DeleteMapping("/{idPr}/instances/{idInst}")
    public void deleteInstanceForProduct(@PathVariable int idPr, @PathVariable int idInst)  {
        productService.deleteInstanceForProductById(idPr, idInst);
    }

    @PostMapping("/scan/{barcode}")
    public ProductDto findOrCreateByBarcode(@PathVariable String barcode) {
            return productService.findOrCreateByBarcode(barcode);
    }

}
