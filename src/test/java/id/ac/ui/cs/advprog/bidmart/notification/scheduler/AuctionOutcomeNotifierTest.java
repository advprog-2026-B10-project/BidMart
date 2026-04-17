package id.ac.ui.cs.advprog.bidmart.notification.scheduler;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionOutcomeNotifierTest {

    @Mock AuctionRepository auctionRepository;
    @Mock AuctionNotificationStateRepository stateRepository;
    @Mock NotificationService notificationService;
    @InjectMocks AuctionOutcomeNotifier notifier;

    private Bid bid(String buyer, double amount) {
        Bid b = new Bid();
        b.setBuyerId(buyer);
        b.setAmount(amount);
        return b;
    }

    @Test
    void tick_wonAuction_notifiesWinnerAndLosersOnce() {
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setStatus(AuctionStatus.WON);
        List<Bid> bids = new ArrayList<>();
        bids.add(bid("alice@x", 100.0));
        bids.add(bid("bob@x", 200.0));
        bids.add(bid("alice@x", 150.0));
        bids.add(bid("carol@x", 180.0));
        auction.setBids(bids);

        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(auction));
        when(stateRepository.findById(1L)).thenReturn(Optional.empty());

        notifier.tick();

        verify(notificationService).send(eq("bob@x"), eq(NotificationType.AUCTION_WON),
                anyString(), anyString(), eq("1"));
        verify(notificationService).send(eq("alice@x"), eq(NotificationType.AUCTION_LOST),
                anyString(), anyString(), eq("1"));
        verify(notificationService).send(eq("carol@x"), eq(NotificationType.AUCTION_LOST),
                anyString(), anyString(), eq("1"));
        verify(stateRepository).save(argThat(s ->
                s.getAuctionId().equals(1L) && s.getLastNotifiedStatus() == AuctionStatus.WON));
    }

    @Test
    void tick_skipsAlreadyNotifiedTerminalStatus() {
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setStatus(AuctionStatus.WON);
        auction.setBids(List.of(bid("bob@x", 200.0)));

        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(auction));
        when(stateRepository.findById(1L)).thenReturn(Optional.of(
                AuctionNotificationState.builder()
                        .auctionId(1L)
                        .lastNotifiedStatus(AuctionStatus.WON).build()));

        notifier.tick();

        verify(notificationService, never()).send(any(), any(), any(), any(), any());
        verify(stateRepository, never()).save(any());
    }

    @Test
    void tick_unsoldAuction_notifiesAllBidders() {
        Auction auction = new Auction();
        auction.setId(2L);
        auction.setStatus(AuctionStatus.UNSOLD);
        auction.setBids(List.of(bid("alice@x", 30.0), bid("bob@x", 40.0)));

        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(auction));
        when(stateRepository.findById(2L)).thenReturn(Optional.empty());

        notifier.tick();

        verify(notificationService).send(eq("alice@x"), eq(NotificationType.AUCTION_UNSOLD),
                anyString(), anyString(), eq("2"));
        verify(notificationService).send(eq("bob@x"), eq(NotificationType.AUCTION_UNSOLD),
                anyString(), anyString(), eq("2"));
    }

    @Test
    void tick_unsoldAuctionWithNoBids_noNotificationsStillRecordsState() {
        Auction auction = new Auction();
        auction.setId(3L);
        auction.setStatus(AuctionStatus.UNSOLD);
        auction.setBids(List.of());

        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(auction));
        when(stateRepository.findById(3L)).thenReturn(Optional.empty());

        notifier.tick();

        verify(notificationService, never()).send(any(), any(), any(), any(), any());
        verify(stateRepository).save(argThat(s ->
                s.getAuctionId().equals(3L) && s.getLastNotifiedStatus() == AuctionStatus.UNSOLD));
    }
}
