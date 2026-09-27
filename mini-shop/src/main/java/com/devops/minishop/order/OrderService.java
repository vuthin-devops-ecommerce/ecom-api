package com.devops.minishop.order;

import com.devops.minishop.catalog.ProductService;
import com.devops.minishop.catalog.dto.ProductResponse;
import com.devops.minishop.common.BadRequestException;
import com.devops.minishop.common.InsufficientStockException;
import com.devops.minishop.common.NotFoundException;
import com.devops.minishop.order.dto.CreateOrderRequest;
import com.devops.minishop.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        rejectDuplicateProducts(request);

        // 1. Load every product first (404 if any is missing) - nothing has been written yet.
        Map<Long, ProductResponse> products = new LinkedHashMap<>();
        for (CreateOrderRequest.Item item : request.items()) {
            products.put(item.productId(), productService.getById(item.productId()));
        }

        // 2. Check stock for ALL lines before decrementing ANY of them.
        for (CreateOrderRequest.Item item : request.items()) {
            ProductResponse product = products.get(item.productId());
            if (product.stock() < item.quantity()) {
                throw new InsufficientStockException("Product " + product.id() + " (" + product.sku()
                        + "): requested " + item.quantity() + ", available " + product.stock());
            }
        }

        // 3. Decrement - joins this transaction, so any later failure rolls these back too.
        for (CreateOrderRequest.Item item : request.items()) {
            productService.decreaseStock(item.productId(), item.quantity());
        }

        // 4. Build the order with price snapshots taken from step 1.
        Order order = new Order();
        for (CreateOrderRequest.Item item : request.items()) {
            order.addItem(new OrderItem(item.productId(), item.quantity(), products.get(item.productId()).price()));
        }
        order.setTotalAmount(order.getItems().stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse getById(Long id) {
        return OrderResponse.from(orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("Order", id)));
    }

    private static void rejectDuplicateProducts(CreateOrderRequest request) {
        Set<Long> seen = new HashSet<>();
        for (CreateOrderRequest.Item item : request.items()) {
            if (!seen.add(item.productId())) {
                throw new BadRequestException("Duplicate productId in order: " + item.productId());
            }
        }
    }
}
