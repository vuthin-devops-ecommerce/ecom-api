package com.devops.minishop.order;

import com.devops.minishop.catalog.Product;
import com.devops.minishop.catalog.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real PostgreSQL 17 in Docker (pinned, same as compose/K8s later). Profile 'it' keeps the dev seed out.
@SpringBootTest(properties = "spring.profiles.active=it")
@AutoConfigureMockMvc
class OrderControllerIT {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    static {
        postgres.start();
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository productRepository;

    @MockitoSpyBean
    OrderRepository orderRepository;

    private Product seed(String sku, String price, int stock) {
        return productRepository.save(new Product(sku, "Product " + sku, new BigDecimal(price), stock));
    }

    private int stockInDb(Long id) {
        return productRepository.findById(id).orElseThrow().getStock();
    }

    private static String orderJson(Long p1, int q1, Long p2, int q2) {
        return """
                {"items":[{"productId":%d,"quantity":%d},{"productId":%d,"quantity":%d}]}
                """.formatted(p1, q1, p2, q2);
    }

    @Test
    void postOrder_decrementsStockInDatabase() throws Exception {
        Product a = seed("IT-OK-A", "9.99", 10);
        Product b = seed("IT-OK-B", "19.50", 5);

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(a.getId(), 2, b.getId(), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(39.48))
                .andExpect(jsonPath("$.items.length()").value(2));

        assertThat(stockInDb(a.getId())).isEqualTo(8);
        assertThat(stockInDb(b.getId())).isEqualTo(4);
    }

    @Test
    void postOrder_insufficientStock_returns400AndLeavesStockUnchanged() throws Exception {
        Product a = seed("IT-LOW-A", "9.99", 10);
        Product b = seed("IT-LOW-B", "19.50", 1);

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(a.getId(), 1, b.getId(), 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("IT-LOW-B")));

        assertThat(stockInDb(a.getId())).isEqualTo(10);
        assertThat(stockInDb(b.getId())).isEqualTo(1);
    }

    @Test
    void postOrder_failureAfterStockDecrement_rollsBackStock() throws Exception {
        Product a = seed("IT-RB-A", "9.99", 10);
        Product b = seed("IT-RB-B", "19.50", 5);
        // Stock checks pass and decrements happen, then persisting the order blows up.
        doThrow(new RuntimeException("simulated DB failure")).when(orderRepository).save(any(Order.class));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(a.getId(), 2, b.getId(), 1)))
                .andExpect(status().isInternalServerError());

        // The decrements were executed inside the same transaction, so they must be gone.
        assertThat(stockInDb(a.getId())).isEqualTo(10);
        assertThat(stockInDb(b.getId())).isEqualTo(5);
    }
}
