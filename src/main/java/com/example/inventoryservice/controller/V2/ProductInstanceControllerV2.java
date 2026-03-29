package com.example.inventoryservice.controller.V2;


import com.example.inventoryservice.dto.ProductInstanceDto;
import com.example.inventoryservice.dto.TransferProductDto;
import com.example.inventoryservice.service.V2.ProductInstanceServiceV2;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/instances")
public class ProductInstanceControllerV2 {

    private final ProductInstanceServiceV2 productInstanceService;

    @GetMapping()
    public List<ProductInstanceDto> getAllInstancesForUser(@RequestParam Integer userId) {
        return productInstanceService.findInstancesForUser(userId);
    }

    @GetMapping("/{id}")
    public ProductInstanceDto getProductInstance(@PathVariable int id,
                                                 @RequestParam Integer userId) {
        return productInstanceService.findProductInstanceById(id, userId);
    }

    @PostMapping()
    public ProductInstanceDto createProductInstance(@Valid @RequestBody ProductInstanceDto productInstanceDto,
                                                    @RequestParam Integer userId) {
        return productInstanceService.createProductInstanceForUser(productInstanceDto, userId);
    }

    @PutMapping("/{id}")
    public ProductInstanceDto changeProductInstance(@PathVariable int id,
                                                    @RequestParam Integer userId,
                                                    @Valid @RequestBody ProductInstanceDto productInstanceDto) {
        return productInstanceService.updateProductInstance(id, userId, productInstanceDto);
    }

    @DeleteMapping("/{id}")
    public void deleteProductInstance(@PathVariable int id,
                                      @RequestParam int userId)  {
        productInstanceService.deleteProductInstanceById(id, userId);
    }

    @GetMapping("/expiring")
    public List<TransferProductDto> getExpiring(@RequestParam int userId) {
        return productInstanceService.getExpiring(userId);
    }

}
