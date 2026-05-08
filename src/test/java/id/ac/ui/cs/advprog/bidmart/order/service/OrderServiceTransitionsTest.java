package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.order.entity.Order;
import id.ac.ui.cs.advprog.bidmart.order.entity.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTransitionsTest {

    @Mock OrderRepository orderRepository;
    @Mock NotificationService notificationService;
    @InjectMocks OrderService service;

    private Order order(Long id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .auctionId(7L)
                .buyerId("buyer@x")
                .sellerId("seller@x")
                .totalAmount(100.0)
                .status(status)
                .build();
    }

    // --- setShippingAddress ---

    @Test
    void setShippingAddress_buyerOnPending_updatesAddress() {
        Order existing = order(1L, OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = service.setShippingAddress(1L, "buyer@x", "Jl. Mawar No. 1");

        assertEquals("Jl. Mawar No. 1", result.getShippingAddress());
        assertEquals(OrderStatus.PENDING, result.getStatus());
    }

    @Test
    void setShippingAddress_buyerOnConfirmed_updatesAddress() {
        Order existing = order(1L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = service.setShippingAddress(1L, "buyer@x", "Jl. Mawar No. 1");

        assertEquals("Jl. Mawar No. 1", result.getShippingAddress());
    }

    @Test
    void setShippingAddress_rejectsNonBuyer() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.PENDING)));

        assertThrows(SecurityException.class,
                () -> service.setShippingAddress(1L, "seller@x", "x"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void setShippingAddress_rejectsAfterShipped() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.SHIPPED)));

        assertThrows(IllegalStateException.class,
                () -> service.setShippingAddress(1L, "buyer@x", "x"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void setShippingAddress_rejectsMissingOrder() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.setShippingAddress(99L, "buyer@x", "x"));
    }

    // --- confirmOrder ---

    @Test
    void confirmOrder_sellerOnPending_setsConfirmed() {
        Order existing = order(1L, OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = service.confirmOrder(1L, "seller@x");

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
        verify(notificationService).send(
                eq("buyer@x"),
                eq(NotificationType.ORDER_CONFIRMED),
                anyString(),
                anyString(),
                eq("1"));
    }

    @Test
    void confirmOrder_rejectsNonSeller() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.PENDING)));

        assertThrows(SecurityException.class,
                () -> service.confirmOrder(1L, "buyer@x"));
        verify(orderRepository, never()).save(any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void confirmOrder_rejectsAlreadyConfirmed() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.CONFIRMED)));

        assertThrows(IllegalStateException.class,
                () -> service.confirmOrder(1L, "seller@x"));
        verify(orderRepository, never()).save(any());
    }

    // --- shipOrder ---

    @Test
    void shipOrder_sellerOnConfirmed_withAddressAndTracking_setsShipped() {
        Order existing = order(1L, OrderStatus.CONFIRMED);
        existing.setShippingAddress("Jl. Mawar No. 1");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = service.shipOrder(1L, "seller@x", "JNE12345");

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        assertEquals("JNE12345", result.getTrackingNumber());
        assertNotNull(result.getShippedAt());
        verify(notificationService).send(
                eq("buyer@x"),
                eq(NotificationType.ORDER_SHIPPED),
                anyString(),
                anyString(),
                eq("1"));
    }

    @Test
    void shipOrder_rejectsNonSeller() {
        Order existing = order(1L, OrderStatus.CONFIRMED);
        existing.setShippingAddress("Jl. Mawar No. 1");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class,
                () -> service.shipOrder(1L, "buyer@x", "JNE12345"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shipOrder_rejectsFromPending() {
        Order existing = order(1L, OrderStatus.PENDING);
        existing.setShippingAddress("Jl. Mawar No. 1");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> service.shipOrder(1L, "seller@x", "JNE12345"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shipOrder_rejectsWhenShippingAddressMissing() {
        Order existing = order(1L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> service.shipOrder(1L, "seller@x", "JNE12345"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shipOrder_rejectsBlankTrackingNumber() {
        Order existing = order(1L, OrderStatus.CONFIRMED);
        existing.setShippingAddress("Jl. Mawar No. 1");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> service.shipOrder(1L, "seller@x", "  "));
        verify(orderRepository, never()).save(any());
    }

    // --- receiveOrder ---

    @Test
    void receiveOrder_buyerOnShipped_setsDelivered() {
        Order existing = order(1L, OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = service.receiveOrder(1L, "buyer@x");

        assertEquals(OrderStatus.DELIVERED, result.getStatus());
        assertNotNull(result.getDeliveredAt());
        verify(notificationService).send(
                eq("seller@x"),
                eq(NotificationType.ORDER_DELIVERED),
                anyString(),
                anyString(),
                eq("1"));
    }

    @Test
    void receiveOrder_rejectsNonBuyer() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.SHIPPED)));

        assertThrows(SecurityException.class,
                () -> service.receiveOrder(1L, "seller@x"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void receiveOrder_rejectsFromConfirmed() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.CONFIRMED)));

        assertThrows(IllegalStateException.class,
                () -> service.receiveOrder(1L, "buyer@x"));
        verify(orderRepository, never()).save(any());
    }
}
