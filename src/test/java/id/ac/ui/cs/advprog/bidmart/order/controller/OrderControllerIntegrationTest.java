package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.auth.service.EmailService;
import id.ac.ui.cs.advprog.bidmart.order.entity.Order;
import id.ac.ui.cs.advprog.bidmart.order.entity.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class OrderControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;

    @MockBean EmailService emailService;

    @BeforeEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    private Long seed(Long auctionId, String buyer) {
        Order o = Order.builder()
                .auctionId(auctionId).buyerId(buyer).sellerId("seller@x")
                .totalAmount(100.0).status(OrderStatus.PENDING).build();
        return orderRepository.save(o).getId();
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getMine_returnsOnlyMyOrders() throws Exception {
        seed(1L, "alice@x");
        seed(2L, "alice@x");
        seed(3L, "bob@x");

        mockMvc.perform(get("/orders/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getById_rejectsOthersOrder() throws Exception {
        Long id = seed(10L, "bob@x");
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getById_returnsMyOrder() throws Exception {
        Long id = seed(10L, "alice@x");
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId", is(10)));
    }
}
