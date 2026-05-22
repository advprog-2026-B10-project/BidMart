package id.ac.ui.cs.advprog.bidmart.notification.listener;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.event.AuctionEndedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionOutcomeListenerTest {

    @Mock AuctionRepository auctionRepository;
    @Mock OrderService orderService;
    @Mock NotificationService notificationService;
    @InjectMocks AuctionOutcomeListener listener;

    private Bid bid(String buyer, double amount) {
        Bid b = new Bid();
        b.setBuyerId(buyer);
        b.setAmount(amount);
        return b;
    }

    private Auction auction(Long id, AuctionStatus status, List<Bid> bids) {
        Auction a = new Auction();
        a.setId(id);
        a.setSellerId("seller@x");
        a.setStatus(status);
        a.setBids(new ArrayList<>(bids));
        return a;
    }

    @Test
    void onAuctionEnded_won_createsOrderAndNotifiesWinnerAndLosers() {
        Auction a = auction(1L, AuctionStatus.WON, List.of(
                bid("alice@x", 100.0),
                bid("bob@x", 200.0),
                bid("carol@x", 180.0)));
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(a));

        listener.onAuctionEnded(new AuctionEndedEvent(this, 1L, "bob@x", 200.0, AuctionStatus.WON));

        verify(orderService).createFromWonAuction(a);
        verify(notificationService).send(eq("bob@x"), eq(NotificationType.AUCTION_WON),
                anyString(), anyString(), eq("1"));
        verify(notificationService).send(eq("alice@x"), eq(NotificationType.AUCTION_LOST),
                anyString(), anyString(), eq("1"));
        verify(notificationService).send(eq("carol@x"), eq(NotificationType.AUCTION_LOST),
                anyString(), anyString(), eq("1"));
    }

    @Test
    void onAuctionEnded_unsold_notifiesAllBidders() {
        Auction a = auction(2L, AuctionStatus.UNSOLD, List.of(
                bid("alice@x", 30.0),
                bid("bob@x", 40.0)));
        when(auctionRepository.findById(2L)).thenReturn(Optional.of(a));

        listener.onAuctionEnded(new AuctionEndedEvent(this, 2L, null, 40.0, AuctionStatus.UNSOLD));

        verify(notificationService).send(eq("alice@x"), eq(NotificationType.AUCTION_UNSOLD),
                anyString(), anyString(), eq("2"));
        verify(notificationService).send(eq("bob@x"), eq(NotificationType.AUCTION_UNSOLD),
                anyString(), anyString(), eq("2"));
        verify(orderService, never()).createFromWonAuction(any());
    }

    @Test
    void onAuctionEnded_unsoldWithNoBids_doesNothing() {
        Auction a = auction(3L, AuctionStatus.UNSOLD, List.of());
        when(auctionRepository.findById(3L)).thenReturn(Optional.of(a));

        listener.onAuctionEnded(new AuctionEndedEvent(this, 3L, null, 0.0, AuctionStatus.UNSOLD));

        verify(notificationService, never()).send(any(), any(), any(), any(), any());
        verify(orderService, never()).createFromWonAuction(any());
    }

    @Test
    void onAuctionEnded_auctionMissing_logsAndSkips() {
        when(auctionRepository.findById(99L)).thenReturn(Optional.empty());

        listener.onAuctionEnded(new AuctionEndedEvent(this, 99L, "bob@x", 100.0, AuctionStatus.WON));

        verify(orderService, never()).createFromWonAuction(any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void onAuctionEnded_exceptionInHandlerIsSwallowed() {
        Auction a = auction(4L, AuctionStatus.WON, List.of(bid("bob@x", 100.0)));
        when(auctionRepository.findById(4L)).thenReturn(Optional.of(a));
        when(orderService.createFromWonAuction(a)).thenThrow(new RuntimeException("boom"));

        // Should not propagate
        listener.onAuctionEnded(new AuctionEndedEvent(this, 4L, "bob@x", 100.0, AuctionStatus.WON));
    }
}
