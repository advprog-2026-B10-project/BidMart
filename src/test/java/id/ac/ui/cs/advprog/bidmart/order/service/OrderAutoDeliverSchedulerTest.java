package id.ac.ui.cs.advprog.bidmart.order.service;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAutoDeliverSchedulerTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderService orderService;
    @InjectMocks OrderAutoDeliverScheduler scheduler;

    @BeforeEach
    void setWindow() {
        ReflectionTestUtils.setField(scheduler, "autoDeliverAfterDays", 7);
    }

    @Test
    void tick_callsAutoDeliverForEachStaleOrder() {
        Order o1 = Order.builder().id(10L).status(OrderStatus.SHIPPED).build();
        Order o2 = Order.builder().id(11L).status(OrderStatus.SHIPPED).build();
        when(orderRepository.findByStatusAndShippedAtBefore(eq(OrderStatus.SHIPPED), any(LocalDateTime.class)))
                .thenReturn(List.of(o1, o2));

        scheduler.tick();

        verify(orderService).autoDeliverOrder(10L);
        verify(orderService).autoDeliverOrder(11L);
    }

    @Test
    void tick_usesCutoffSevenDaysAgo() {
        when(orderRepository.findByStatusAndShippedAtBefore(eq(OrderStatus.SHIPPED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusDays(7).minusSeconds(2);
        scheduler.tick();
        LocalDateTime after = LocalDateTime.now().minusDays(7).plusSeconds(2);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findByStatusAndShippedAtBefore(eq(OrderStatus.SHIPPED), cutoffCaptor.capture());
        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertTrue(cutoff.isAfter(before) && cutoff.isBefore(after));
    }

    @Test
    void tick_swallowsExceptionFromOneOrder_continuesWithRest() {
        Order o1 = Order.builder().id(10L).status(OrderStatus.SHIPPED).build();
        Order o2 = Order.builder().id(11L).status(OrderStatus.SHIPPED).build();
        when(orderRepository.findByStatusAndShippedAtBefore(eq(OrderStatus.SHIPPED), any(LocalDateTime.class)))
                .thenReturn(List.of(o1, o2));
        when(orderService.autoDeliverOrder(10L)).thenThrow(new IllegalStateException("boom"));

        scheduler.tick();

        verify(orderService).autoDeliverOrder(10L);
        verify(orderService).autoDeliverOrder(11L);
        verify(orderService, times(2)).autoDeliverOrder(any());
    }

    @Test
    void tick_noStaleOrders_doesNothing() {
        when(orderRepository.findByStatusAndShippedAtBefore(eq(OrderStatus.SHIPPED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.tick();

        verify(orderService, never()).autoDeliverOrder(any());
    }
}
