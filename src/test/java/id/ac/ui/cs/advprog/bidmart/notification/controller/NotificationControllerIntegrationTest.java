package id.ac.ui.cs.advprog.bidmart.notification.controller;

import id.ac.ui.cs.advprog.bidmart.auth.service.EmailService;
import id.ac.ui.cs.advprog.bidmart.notification.entity.Notification;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationChannel;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired NotificationRepository repository;

    @MockBean EmailService emailService;

    @BeforeEach
    void cleanup() {
        repository.deleteAll();
    }

    private Long seed(String user, boolean read) {
        Notification n = Notification.builder()
                .userId(user).title("t").message("m")
                .type(NotificationType.SYSTEM).channel(NotificationChannel.IN_APP)
                .isRead(read).build();
        return repository.save(n).getId();
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getMine_returnsOnlyMyNotifications() throws Exception {
        seed("alice@x", false);
        seed("alice@x", true);
        seed("bob@x", false);

        mockMvc.perform(get("/notifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void unreadCount_returnsCount() throws Exception {
        seed("alice@x", false);
        seed("alice@x", false);
        seed("alice@x", true);

        mockMvc.perform(get("/notifications/me/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void markRead_flipsIsRead() throws Exception {
        Long id = seed("alice@x", false);

        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void markRead_cannotAffectAnotherUsersNotification() throws Exception {
        Long id = seed("bob@x", false);

        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().is4xxClientError());
    }
}
