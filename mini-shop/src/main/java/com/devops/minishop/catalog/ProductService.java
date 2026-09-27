package com.devops.minishop.catalog;

import com.devops.minishop.catalog.dto.CreateProductRequest;
import com.devops.minishop.catalog.dto.ProductResponse;
import com.devops.minishop.common.ConflictException;
import com.devops.minishop.common.InsufficientStockException;
import com.devops.minishop.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new ConflictException("SKU already exists: " + request.sku());
        }
        Product product = new Product(request.sku(), request.name(), request.price(), request.stock());
        return ProductResponse.from(repository.save(product));
    }

    public List<ProductResponse> findAll() {
        return repository.findAll().stream().map(ProductResponse::from).toList();
    }

    public ProductResponse getById(Long id) {
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional
    public ProductResponse adjustStock(Long id, int delta) {
        Product product = requireProduct(id);
        int newStock = product.getStock() + delta;
        if (newStock < 0) {
            throw new InsufficientStockException(
                    "Product " + id + ": stock " + product.getStock() + " cannot be adjusted by " + delta);
        }
        product.setStock(newStock);
        return ProductResponse.from(product);
    }

    @Transactional
    public void decreaseStock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0, was " + quantity);
        }
        Product product = requireProduct(id);
        if (product.getStock() < quantity) {
            throw new InsufficientStockException(
                    "Product " + id + ": requested " + quantity + ", available " + product.getStock());
        }
        product.setStock(product.getStock() - quantity);
    }

    private Product requireProduct(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }
}
