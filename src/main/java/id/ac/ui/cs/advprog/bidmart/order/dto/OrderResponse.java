package id.ac.ui.cs.advprog.bidmart.order.dto;

import id.ac.ui.cs.advprog.bidmart.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long auctionId;
    private String buyerId;
    private String sellerId;
    private Double totalAmount;
    private String status;
    private String shippingAddress;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(), o.getAuctionId(), o.getBuyerId(), o.getSellerId(),
                o.getTotalAmount(), o.getStatus().name(),
                o.getShippingAddress(), o.getTrackingNumber(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
