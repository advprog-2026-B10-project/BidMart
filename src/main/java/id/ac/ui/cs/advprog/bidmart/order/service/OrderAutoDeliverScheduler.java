package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.entity.Order;
import id.ac.ui.cs.advprog.bidmart.order.entity.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoDeliverScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${app.order.auto-deliver-after-days:7}")
    private int autoDeliverAfterDays;

    @Scheduled(fixedRate = 3_600_000)
    public void tick() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(autoDeliverAfterDays);
        List<Order> stale = orderRepository.findByStatusAndShippedAtBefore(OrderStatus.SHIPPED, cutoff);
        for (Order o : stale) {
            try {
                orderService.autoDeliverOrder(o.getId());
            } catch (Exception e) {
                log.error("Failed to auto-deliver order {}: {}", o.getId(), e.getMessage());
            }
        }
    }
}
