package id.ac.ui.cs.advprog.bidmart.notification.service;

import id.ac.ui.cs.advprog.bidmart.notification.entity.Notification;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationChannel;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationPreference;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceService preferenceService;
    @InjectMocks NotificationService service;

    private NotificationPreference allOn(String userId) {
        return NotificationPreference.builder()
                .userId(userId)
                .inAppEnabled(true).auctionStatusEnabled(true)
                .orderUpdateEnabled(true).transactionEnabled(true)
                .systemEnabled(true).build();
    }

    @Test
    void send_persistsWhenPreferencesAllow() {
        when(preferenceService.getOrDefault("alice@x")).thenReturn(allOn("alice@x"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        service.send("alice@x", NotificationType.AUCTION_WON, "Menang!", "Kamu memenangkan lelang #42", "42");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("alice@x", saved.getUserId());
        assertEquals(NotificationType.AUCTION_WON, saved.getType());
        assertEquals(NotificationChannel.IN_APP, saved.getChannel());
        assertEquals("Menang!", saved.getTitle());
        assertEquals("42", saved.getReferenceId());
        assertFalse(saved.getIsRead());
    }

    @Test
    void send_skipsWhenAuctionPrefDisabled() {
        NotificationPreference pref = allOn("alice@x");
        pref.setAuctionStatusEnabled(false);
        when(preferenceService.getOrDefault("alice@x")).thenReturn(pref);

        service.send("alice@x", NotificationType.AUCTION_WON, "x", "y", null);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void send_skipsWhenInAppDisabled() {
        NotificationPreference pref = allOn("alice@x");
        pref.setInAppEnabled(false);
        when(preferenceService.getOrDefault("alice@x")).thenReturn(pref);

        service.send("alice@x", NotificationType.SYSTEM, "x", "y", null);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_setsIsReadAndReadAt() {
        Notification existing = Notification.builder()
                .id(5L).userId("alice@x").title("t").message("m")
                .type(NotificationType.SYSTEM).channel(NotificationChannel.IN_APP)
                .isRead(false).build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = service.markAsRead(5L, "alice@x");

        assertTrue(result.getIsRead());
        assertNotNull(result.getReadAt());
    }

    @Test
    void markAsRead_rejectsWhenUserMismatch() {
        Notification existing = Notification.builder()
                .id(5L).userId("bob@x").title("t").message("m")
                .type(NotificationType.SYSTEM).channel(NotificationChannel.IN_APP)
                .isRead(false).build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class, () -> service.markAsRead(5L, "alice@x"));
        verify(notificationRepository, never()).save(any());
    }
}
