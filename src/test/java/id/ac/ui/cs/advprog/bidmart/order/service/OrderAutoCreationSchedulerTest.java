package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAutoCreationSchedulerTest {

    @Mock AuctionRepository auctionRepository;
    @Mock OrderService orderService;
    @InjectMocks OrderAutoCreationScheduler scheduler;

    @Test
    void tick_callsOrderServiceForEachWonAuction() {
        Auction a1 = new Auction(); a1.setId(1L); a1.setStatus(AuctionStatus.WON);
        Auction a2 = new Auction(); a2.setId(2L); a2.setStatus(AuctionStatus.WON);
        when(auctionRepository.findByStatusIn(List.of(AuctionStatus.WON)))
                .thenReturn(List.of(a1, a2));

        scheduler.tick();

        verify(orderService).createFromWonAuction(a1);
        verify(orderService).createFromWonAuction(a2);
    }

    @Test
    void tick_swallowsExceptionFromOneAuctionAndContinues() {
        Auction a1 = new Auction(); a1.setId(1L); a1.setStatus(AuctionStatus.WON);
        Auction a2 = new Auction(); a2.setId(2L); a2.setStatus(AuctionStatus.WON);
        when(auctionRepository.findByStatusIn(List.of(AuctionStatus.WON)))
                .thenReturn(List.of(a1, a2));
        when(orderService.createFromWonAuction(a1)).thenThrow(new RuntimeException("boom"));

        scheduler.tick();

        verify(orderService).createFromWonAuction(a2);
    }
}
