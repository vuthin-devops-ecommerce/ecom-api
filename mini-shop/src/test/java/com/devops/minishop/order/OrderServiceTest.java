package com.devops.minishop.order;

import com.devops.minishop.catalog.ProductService;
import com.devops.minishop.catalog.dto.ProductResponse;
import com.devops.minishop.common.BadRequestException;
import com.devops.minishop.common.InsufficientStockException;
import com.devops.minishop.common.NotFoundException;
import com.devops.minishop.order.dto.CreateOrderRequest;
import com.devops.minishop.order.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    ProductService productService;

    @InjectMocks
    OrderService service;

    private static ProductResponse product(long id, String price, int stock) {
        return new ProductResponse(id, "SKU-" + id, "Product " + id, new BigDecimal(price), stock, null);
    }

    private static CreateOrderRequest order(CreateOrderRequest.Item... items) {
        return new CreateOrderRequest(List.of(items));
    }

    @Test
    void createOrder_stockAvailable_createsOrderAndDecreasesStock() {
        when(productService.getById(1L)).thenReturn(product(1, "9.99", 10));
        when(productService.getById(2L)).thenReturn(product(2, "19.50", 5));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        OrderResponse response = service.createOrder(order(
                new CreateOrderRequest.Item(1L, 2),
                new CreateOrderRequest.Item(2L, 1)));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.totalAmount()).isEqualByComparingTo("99.99");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).priceAtOrder()).isEqualByComparingTo("9.99");
        verify(productService).decreaseStock(1L, 2);
        verify(productService).decreaseStock(2L, 1);
    }

    @Test
    void createOrder_insufficientStockOnSecondItem_decreasesNothing() {
        when(productService.getById(1L)).thenReturn(product(1, "9.99", 10));
        when(productService.getById(2L)).thenReturn(product(2, "19.50", 0));

        assertThatThrownBy(() -> service.createOrder(order(
                new CreateOrderRequest.Item(1L, 1),
                new CreateOrderRequest.Item(2L, 1))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Product 2");

        // The rule under test: nothing is decremented unless every line passes the check.
        verify(productService, never()).decreaseStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_unknownProduct_throwsNotFoundBeforeAnyWrite() {
        when(productService.getById(1L)).thenReturn(product(1, "9.99", 10));
        when(productService.getById(999L)).thenThrow(new NotFoundException("Product", 999L));

        assertThatThrownBy(() -> service.createOrder(order(
                new CreateOrderRequest.Item(1L, 1),
                new CreateOrderRequest.Item(999L, 1))))
                .isInstanceOf(NotFoundException.class);

        verify(productService, never()).decreaseStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_duplicateProductId_rejectedBeforeLookup() {
        assertThatThrownBy(() -> service.createOrder(order(
                new CreateOrderRequest.Item(1L, 1),
                new CreateOrderRequest.Item(1L, 2))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate productId");

        verify(productService, never()).getById(anyLong());
    }
}
