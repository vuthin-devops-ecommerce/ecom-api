package com.devops.minishop.catalog;

import com.devops.minishop.catalog.dto.AdjustStockRequest;
import com.devops.minishop.catalog.dto.CreateProductRequest;
import com.devops.minishop.catalog.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ProductResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse adjustStock(@PathVariable Long id, @Valid @RequestBody AdjustStockRequest request) {
        return service.adjustStock(id, request.delta());
    }
}
