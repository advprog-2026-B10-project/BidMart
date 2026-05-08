package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.order.entity.Order;
import id.ac.ui.cs.advprog.bidmart.order.entity.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional
    public Optional<Order> createFromWonAuction(Auction auction) {
        if (auction.getStatus() != AuctionStatus.WON) {
            throw new IllegalArgumentException("Auction must be WON to create order, was " + auction.getStatus());
        }
        if (orderRepository.existsByAuctionId(auction.getId())) {
            return Optional.empty();
        }
        Bid winner = auction.getBids().stream()
                .max(Comparator.comparingDouble(Bid::getAmount))
                .orElseThrow(() -> new IllegalStateException("WON auction has no bids: " + auction.getId()));

        Order order = Order.builder()
                .auctionId(auction.getId())
                .buyerId(winner.getBuyerId())
                .sellerId(auction.getSellerId())
                .totalAmount(winner.getAmount())
                .status(OrderStatus.PENDING)
                .build();
        Order saved = orderRepository.save(order);

        notificationService.send(
                winner.getBuyerId(),
                NotificationType.ORDER_CREATED,
                "Pesanan Dibuat",
                "Pesanan untuk lelang #" + auction.getId() + " telah dibuat. Total Rp " + winner.getAmount(),
                String.valueOf(saved.getId()));
        return Optional.of(saved);
    }

    public List<Order> findByBuyer(String buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public List<Order> findBySeller(String sellerId) {
        return orderRepository.findBySellerId(sellerId);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order setShippingAddress(Long orderId, String userId, String address) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (!order.getBuyerId().equals(userId)) {
            throw new SecurityException("Only the buyer can set the shipping address");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot set shipping address when order is " + order.getStatus());
        }
        order.setShippingAddress(address);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOrder(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (!order.getSellerId().equals(userId)) {
            throw new SecurityException("Only the seller can confirm the order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm order from status " + order.getStatus());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        notificationService.send(
                saved.getBuyerId(),
                NotificationType.ORDER_CONFIRMED,
                "Pesanan Dikonfirmasi",
                "Penjual telah mengonfirmasi pesanan #" + saved.getId() + ".",
                String.valueOf(saved.getId()));
        return saved;
    }

    @Transactional
    public Order shipOrder(Long orderId, String userId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (!order.getSellerId().equals(userId)) {
            throw new SecurityException("Only the seller can mark the order as shipped");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot ship order from status " + order.getStatus());
        }
        if (order.getShippingAddress() == null || order.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException(
                    "Cannot ship order without a shipping address");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("Tracking number must not be blank");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order.setShippedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        notificationService.send(
                saved.getBuyerId(),
                NotificationType.ORDER_SHIPPED,
                "Pesanan Dikirim",
                "Pesanan #" + saved.getId() + " dikirim. Resi: " + trackingNumber + ".",
                String.valueOf(saved.getId()));
        return saved;
    }

    @Transactional
    public Order receiveOrder(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (!order.getBuyerId().equals(userId)) {
            throw new SecurityException("Only the buyer can confirm receipt");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException(
                    "Cannot confirm receipt from status " + order.getStatus());
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        notificationService.send(
                saved.getSellerId(),
                NotificationType.ORDER_DELIVERED,
                "Pesanan Diterima",
                "Pembeli mengonfirmasi penerimaan pesanan #" + saved.getId() + ".",
                String.valueOf(saved.getId()));
        return saved;
    }

    @Transactional
    public Order disputeOrder(Long orderId, String userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (!order.getBuyerId().equals(userId)) {
            throw new SecurityException("Only the buyer can dispute the order");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException(
                    "Cannot dispute order from status " + order.getStatus());
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Dispute reason must not be blank");
        }
        order.setStatus(OrderStatus.DISPUTED);
        order.setDisputeReason(reason);
        order.setDisputedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        notificationService.send(
                saved.getSellerId(),
                NotificationType.ORDER_DISPUTED,
                "Pesanan Disengketakan",
                "Pembeli mengajukan sengketa pesanan #" + saved.getId() + ": " + reason,
                String.valueOf(saved.getId()));
        return saved;
    }
}
