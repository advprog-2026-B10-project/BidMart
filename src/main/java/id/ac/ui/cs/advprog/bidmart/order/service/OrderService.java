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

import java.util.Comparator;
import java.util.List;
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

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }
}
