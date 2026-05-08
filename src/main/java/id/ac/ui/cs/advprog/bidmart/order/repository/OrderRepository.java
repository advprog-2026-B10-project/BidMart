package id.ac.ui.cs.advprog.bidmart.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import id.ac.ui.cs.advprog.bidmart.order.entity.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId(String buyerId);
    List<Order> findBySellerId(String sellerId);
    Optional<Order> findByAuctionId(Long auctionId);
    boolean existsByAuctionId(Long auctionId);
}
