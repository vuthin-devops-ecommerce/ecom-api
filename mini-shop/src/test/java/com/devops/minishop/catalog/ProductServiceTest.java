package com.devops.minishop.catalog;

import com.devops.minishop.catalog.dto.CreateProductRequest;
import com.devops.minishop.catalog.dto.ProductResponse;
import com.devops.minishop.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    @InjectMocks
    ProductService service;

    private final CreateProductRequest request =
            new CreateProductRequest("SKU-1", "Widget", new BigDecimal("9.99"), 5);

    @Test
    void create_savesProduct() {
        when(repository.existsBySku("SKU-1")).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.price()).isEqualByComparingTo("9.99");
        assertThat(response.stock()).isEqualTo(5);
        verify(repository).save(any(Product.class));
    }

    @Test
    void create_duplicateSku_throwsConflictAndDoesNotSave() {
        when(repository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SKU-1");

        verify(repository, never()).save(any());
    }
}
