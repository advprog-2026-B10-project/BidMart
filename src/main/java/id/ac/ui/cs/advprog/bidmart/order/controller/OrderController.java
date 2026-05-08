package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.DisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.ShipOrderRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.ShippingAddressRequest;
import id.ac.ui.cs.advprog.bidmart.order.entity.Order;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/me")
    public List<OrderResponse> getMine(Authentication authentication) {
        return orderService.findByBuyer(authentication.getName()).stream()
                .map(OrderResponse::from).toList();
    }

    @GetMapping("/sales")
    public List<OrderResponse> getSales(Authentication authentication) {
        return orderService.findBySeller(authentication.getName()).stream()
                .map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id, Authentication authentication) {
        Order order = orderService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
        if (!order.getBuyerId().equals(authentication.getName())
                && !order.getSellerId().equals(authentication.getName())) {
            throw new SecurityException("Not your order");
        }
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PatchMapping("/{id}/shipping-address")
    public ResponseEntity<OrderResponse> setShippingAddress(
            @PathVariable Long id,
            @Valid @RequestBody ShippingAddressRequest request,
            Authentication authentication) {
        Order updated = orderService.setShippingAddress(id, authentication.getName(), request.getAddress());
        return ResponseEntity.ok(OrderResponse.from(updated));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirm(
            @PathVariable Long id,
            Authentication authentication) {
        Order updated = orderService.confirmOrder(id, authentication.getName());
        return ResponseEntity.ok(OrderResponse.from(updated));
    }

    @PatchMapping("/{id}/ship")
    public ResponseEntity<OrderResponse> ship(
            @PathVariable Long id,
            @Valid @RequestBody ShipOrderRequest request,
            Authentication authentication) {
        Order updated = orderService.shipOrder(id, authentication.getName(), request.getTrackingNumber());
        return ResponseEntity.ok(OrderResponse.from(updated));
    }

    @PatchMapping("/{id}/receive")
    public ResponseEntity<OrderResponse> receive(
            @PathVariable Long id,
            Authentication authentication) {
        Order updated = orderService.receiveOrder(id, authentication.getName());
        return ResponseEntity.ok(OrderResponse.from(updated));
    }

    @PatchMapping("/{id}/dispute")
    public ResponseEntity<OrderResponse> dispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeRequest request,
            Authentication authentication) {
        Order updated = orderService.disputeOrder(id, authentication.getName(), request.getReason());
        return ResponseEntity.ok(OrderResponse.from(updated));
    }
}
