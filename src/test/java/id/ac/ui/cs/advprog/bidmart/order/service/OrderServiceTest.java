package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.order.entity.Order;
import id.ac.ui.cs.advprog.bidmart.order.entity.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock NotificationService notificationService;
    @InjectMocks OrderService service;

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        auction.setId(42L);
        auction.setListingId(1L);
        auction.setSellerId("seller@x");
        auction.setStartingPrice(10_000.0);
        auction.setReservePrice(50_000.0);
        auction.setStatus(AuctionStatus.WON);
        auction.setBids(new ArrayList<>());
    }

    private Bid bid(String buyer, double amount) {
        Bid b = new Bid();
        b.setBuyerId(buyer);
        b.setAmount(amount);
        return b;
    }

    @Test
    void createFromWonAuction_createsOrderAndNotifiesWinner() {
        auction.getBids().add(bid("alice@x", 60_000.0));
        auction.getBids().add(bid("bob@x", 80_000.0));
        auction.getBids().add(bid("alice@x", 70_000.0));

        when(orderRepository.existsByAuctionId(42L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(99L);
            return o;
        });

        Optional<Order> result = service.createFromWonAuction(auction);

        assertTrue(result.isPresent());
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertEquals(42L, saved.getAuctionId());
        assertEquals("bob@x", saved.getBuyerId());
        assertEquals("seller@x", saved.getSellerId());
        assertEquals(80_000.0, saved.getTotalAmount());
        assertEquals(OrderStatus.PENDING, saved.getStatus());

        verify(notificationService).send(
                eq("bob@x"),
                eq(NotificationType.ORDER_CREATED),
                anyString(),
                contains("lelang"),
                eq("99"));
    }

    @Test
    void createFromWonAuction_skipsWhenOrderAlreadyExists() {
        auction.getBids().add(bid("alice@x", 100_000.0));
        when(orderRepository.existsByAuctionId(42L)).thenReturn(true);

        Optional<Order> result = service.createFromWonAuction(auction);

        assertTrue(result.isEmpty());
        verify(orderRepository, never()).save(any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void createFromWonAuction_rejectsNonWonStatus() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.getBids().add(bid("alice@x", 100_000.0));

        assertThrows(IllegalArgumentException.class, () -> service.createFromWonAuction(auction));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createFromWonAuction_rejectsWhenNoBids() {
        auction.setBids(List.of());
        when(orderRepository.existsByAuctionId(42L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.createFromWonAuction(auction));
    }
}
