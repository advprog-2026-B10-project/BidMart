package id.ac.ui.cs.advprog.bidmart.notification.service;

import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationPreference;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationPreferenceRepository;
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
class NotificationPreferenceServiceTest {

    @Mock NotificationPreferenceRepository repository;
    @InjectMocks NotificationPreferenceService service;

    @Test
    void getOrDefault_whenNotFound_returnsDefaultsWithoutPersisting() {
        when(repository.findByUserId("alice@x")).thenReturn(Optional.empty());

        NotificationPreference pref = service.getOrDefault("alice@x");

        assertEquals("alice@x", pref.getUserId());
        assertTrue(pref.getInAppEnabled());
        assertTrue(pref.getAuctionStatusEnabled());
        assertTrue(pref.getOrderUpdateEnabled());
        assertTrue(pref.getTransactionEnabled());
        assertTrue(pref.getSystemEnabled());
        verify(repository, never()).save(any());
    }

    @Test
    void getOrDefault_whenFound_returnsStoredPreference() {
        NotificationPreference stored = NotificationPreference.builder()
                .userId("bob@x")
                .inAppEnabled(false)
                .auctionStatusEnabled(false)
                .orderUpdateEnabled(true)
                .transactionEnabled(true)
                .systemEnabled(true)
                .build();
        when(repository.findByUserId("bob@x")).thenReturn(Optional.of(stored));

        NotificationPreference pref = service.getOrDefault("bob@x");

        assertFalse(pref.getInAppEnabled());
        assertFalse(pref.getAuctionStatusEnabled());
    }

    @Test
    void upsert_createsWhenMissing() {
        when(repository.findByUserId("alice@x")).thenReturn(Optional.empty());
        when(repository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

        NotificationPreference result = service.upsert("alice@x", false, true, true, false, true);

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(repository).save(captor.capture());
        NotificationPreference saved = captor.getValue();
        assertEquals("alice@x", saved.getUserId());
        assertFalse(saved.getInAppEnabled());
        assertTrue(saved.getAuctionStatusEnabled());
        assertFalse(saved.getTransactionEnabled());
    }

    @Test
    void upsert_updatesWhenExists() {
        NotificationPreference existing = NotificationPreference.builder()
                .id(7L).userId("bob@x")
                .inAppEnabled(true).auctionStatusEnabled(true).orderUpdateEnabled(true)
                .transactionEnabled(true).systemEnabled(true).build();
        when(repository.findByUserId("bob@x")).thenReturn(Optional.of(existing));
        when(repository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

        NotificationPreference result = service.upsert("bob@x", false, false, false, false, false);

        assertEquals(7L, result.getId());
        assertFalse(result.getInAppEnabled());
        assertFalse(result.getSystemEnabled());
    }
}
