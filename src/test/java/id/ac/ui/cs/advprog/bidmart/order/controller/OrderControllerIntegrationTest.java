package id.ac.ui.cs.advprog.bidmart.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.auth.service.EmailService;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationRepository;
import id.ac.ui.cs.advprog.bidmart.order.dto.DisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.ShipOrderRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.ShippingAddressRequest;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
class OrderControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ObjectMapper objectMapper;

    @MockBean EmailService emailService;

    @BeforeEach
    void cleanup() {
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private Long seed(Long auctionId, String buyer, String seller, OrderStatus status) {
        Order o = Order.builder()
                .auctionId(auctionId).buyerId(buyer).sellerId(seller)
                .totalAmount(100.0).status(status).build();
        return orderRepository.save(o).getId();
    }

    private Long seedWithAddress(Long auctionId, String buyer, String seller, OrderStatus status, String address) {
        Order o = Order.builder()
                .auctionId(auctionId).buyerId(buyer).sellerId(seller)
                .totalAmount(100.0).status(status).shippingAddress(address).build();
        return orderRepository.save(o).getId();
    }

    // --- existing GET coverage ---

    @Test
    @WithMockUser(username = "alice@x")
    void getMine_returnsOnlyMyOrders() throws Exception {
        seed(1L, "alice@x", "seller@x", OrderStatus.PENDING);
        seed(2L, "alice@x", "seller@x", OrderStatus.PENDING);
        seed(3L, "bob@x", "seller@x", OrderStatus.PENDING);

        mockMvc.perform(get("/orders/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getById_rejectsOthersOrder() throws Exception {
        Long id = seed(10L, "bob@x", "seller@x", OrderStatus.PENDING);
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getById_returnsMyOrder() throws Exception {
        Long id = seed(10L, "alice@x", "seller@x", OrderStatus.PENDING);
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId", is(10)));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getById_missingOrder_returns404() throws Exception {
        mockMvc.perform(get("/orders/{id}", 99999))
                .andExpect(status().isNotFound());
    }

    // --- /orders/sales ---

    @Test
    @WithMockUser(username = "seller@x")
    void getSales_returnsOnlyMySales() throws Exception {
        seed(1L, "alice@x", "seller@x", OrderStatus.PENDING);
        seed(2L, "bob@x", "seller@x", OrderStatus.PENDING);
        seed(3L, "alice@x", "other-seller@x", OrderStatus.PENDING);

        mockMvc.perform(get("/orders/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- /orders/{id}/shipping-address ---

    @Test
    @WithMockUser(username = "alice@x")
    void setShippingAddress_buyerHappyPath() throws Exception {
        Long id = seed(1L, "alice@x", "seller@x", OrderStatus.PENDING);
        ShippingAddressRequest req = new ShippingAddressRequest();
        req.setAddress("Jl. Mawar No. 1");

        mockMvc.perform(patch("/orders/{id}/shipping-address", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingAddress", is("Jl. Mawar No. 1")));
    }

    @Test
    @WithMockUser(username = "seller@x")
    void setShippingAddress_rejectsNonBuyer() throws Exception {
        Long id = seed(1L, "alice@x", "seller@x", OrderStatus.PENDING);
        ShippingAddressRequest req = new ShippingAddressRequest();
        req.setAddress("Jl. Mawar No. 1");

        mockMvc.perform(patch("/orders/{id}/shipping-address", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@x")
    void setShippingAddress_blankAddress_returns400() throws Exception {
        Long id = seed(1L, "alice@x", "seller@x", OrderStatus.PENDING);
        ShippingAddressRequest req = new ShippingAddressRequest();
        req.setAddress(" ");

        mockMvc.perform(patch("/orders/{id}/shipping-address", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- /orders/{id}/confirm ---

    @Test
    @WithMockUser(username = "seller@x")
    void confirm_sellerHappyPath() throws Exception {
        Long id = seed(1L, "alice@x", "seller@x", OrderStatus.PENDING);

        mockMvc.perform(patch("/orders/{id}/confirm", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void confirm_rejectsNonSeller() throws Exception {
        Long id = seed(1L, "alice@x", "seller@x", OrderStatus.PENDING);

        mockMvc.perform(patch("/orders/{id}/confirm", id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "seller@x")
    void confirm_alreadyConfirmed_returns409() throws Exception {
        Long id = seed(1L, "alice@x", "seller@x", OrderStatus.CONFIRMED);

        mockMvc.perform(patch("/orders/{id}/confirm", id))
                .andExpect(status().isConflict());
    }

    // --- /orders/{id}/ship ---

    @Test
    @WithMockUser(username = "seller@x")
    void ship_sellerHappyPath() throws Exception {
        Long id = seedWithAddress(1L, "alice@x", "seller@x", OrderStatus.CONFIRMED, "Jl. Mawar No. 1");
        ShipOrderRequest req = new ShipOrderRequest();
        req.setTrackingNumber("JNE12345");

        mockMvc.perform(patch("/orders/{id}/ship", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SHIPPED")))
                .andExpect(jsonPath("$.trackingNumber", is("JNE12345")));
    }

    @Test
    @WithMockUser(username = "seller@x")
    void ship_withoutAddress_returns400() throws Exception {
        Long id = seed(1L, "alice@x", "seller@x", OrderStatus.CONFIRMED);
        ShipOrderRequest req = new ShipOrderRequest();
        req.setTrackingNumber("JNE12345");

        mockMvc.perform(patch("/orders/{id}/ship", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "seller@x")
    void ship_fromPending_returns409() throws Exception {
        Long id = seedWithAddress(1L, "alice@x", "seller@x", OrderStatus.PENDING, "Jl. Mawar No. 1");
        ShipOrderRequest req = new ShipOrderRequest();
        req.setTrackingNumber("JNE12345");

        mockMvc.perform(patch("/orders/{id}/ship", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- /orders/{id}/receive ---

    @Test
    @WithMockUser(username = "alice@x")
    void receive_buyerHappyPath() throws Exception {
        Long id = seedWithAddress(1L, "alice@x", "seller@x", OrderStatus.SHIPPED, "Jl. Mawar No. 1");

        mockMvc.perform(patch("/orders/{id}/receive", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELIVERED")));
    }

    @Test
    @WithMockUser(username = "seller@x")
    void receive_rejectsNonBuyer() throws Exception {
        Long id = seedWithAddress(1L, "alice@x", "seller@x", OrderStatus.SHIPPED, "Jl. Mawar No. 1");

        mockMvc.perform(patch("/orders/{id}/receive", id))
                .andExpect(status().isForbidden());
    }

    // --- /orders/{id}/dispute ---

    @Test
    @WithMockUser(username = "alice@x")
    void dispute_buyerHappyPath() throws Exception {
        Long id = seedWithAddress(1L, "alice@x", "seller@x", OrderStatus.SHIPPED, "Jl. Mawar No. 1");
        DisputeRequest req = new DisputeRequest();
        req.setReason("Barang rusak saat tiba");

        mockMvc.perform(patch("/orders/{id}/dispute", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISPUTED")))
                .andExpect(jsonPath("$.disputeReason", is("Barang rusak saat tiba")));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void dispute_fromDelivered_returns409() throws Exception {
        Long id = seedWithAddress(1L, "alice@x", "seller@x", OrderStatus.DELIVERED, "Jl. Mawar No. 1");
        DisputeRequest req = new DisputeRequest();
        req.setReason("Barang rusak");

        mockMvc.perform(patch("/orders/{id}/dispute", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "alice@x")
    void dispute_blankReason_returns400() throws Exception {
        Long id = seedWithAddress(1L, "alice@x", "seller@x", OrderStatus.SHIPPED, "Jl. Mawar No. 1");
        DisputeRequest req = new DisputeRequest();
        req.setReason("   ");

        mockMvc.perform(patch("/orders/{id}/dispute", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
