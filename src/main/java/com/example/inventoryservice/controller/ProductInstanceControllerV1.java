package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.ProductInstanceDto;
import com.example.inventoryservice.service.ProductInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/instances")
public class ProductInstanceControllerV1 {

    private final ProductInstanceService productInstanceService;

    @GetMapping()
    public List<ProductInstanceDto> getAllProductInstances() {
        return productInstanceService.findAllProductInstances();
    }

    @GetMapping("/{id}")
    public ProductInstanceDto getProductInstance(@PathVariable int id) {
        return productInstanceService.findProductInstanceById(id);
    }

    @PostMapping()
    public ProductInstanceDto createProductInstance(@Valid @RequestBody ProductInstanceDto productInstanceDto) {
        return productInstanceService.createProductInstance(productInstanceDto);
    }

    @PutMapping("/{id}")
    public ProductInstanceDto changeProductInstance(@PathVariable int id, @Valid @RequestBody ProductInstanceDto productInstanceDto) {
        return productInstanceService.updateProductInstance(id, productInstanceDto);
    }

    @DeleteMapping("/{id}")
    public void deleteProductInstance(@PathVariable int id)  {
        productInstanceService.deleteProductInstanceById(id);
    }
}
