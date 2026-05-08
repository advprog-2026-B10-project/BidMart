package id.ac.ui.cs.advprog.bidmart.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.auth.service.EmailService;
import id.ac.ui.cs.advprog.bidmart.notification.dto.NotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationPreferenceRepository;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationPreferenceControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired NotificationPreferenceRepository repository;
    @Autowired ObjectMapper objectMapper;

    @MockBean EmailService emailService;

    @BeforeEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    @WithMockUser(username = "alice@x")
    void getPreferences_whenNone_returnsDefaults() throws Exception {
        mockMvc.perform(get("/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled", is(true)))
                .andExpect(jsonPath("$.auctionStatusEnabled", is(true)));
    }

    @Test
    @WithMockUser(username = "alice@x")
    void putPreferences_persistsAndReturnsSaved() throws Exception {
        NotificationPreferenceRequest req = new NotificationPreferenceRequest();
        req.setInAppEnabled(false);
        req.setAuctionStatusEnabled(false);
        req.setOrderUpdateEnabled(true);
        req.setTransactionEnabled(false);
        req.setSystemEnabled(true);

        mockMvc.perform(put("/notifications/preferences")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled", is(false)))
                .andExpect(jsonPath("$.auctionStatusEnabled", is(false)))
                .andExpect(jsonPath("$.orderUpdateEnabled", is(true)));

        mockMvc.perform(get("/notifications/preferences"))
                .andExpect(jsonPath("$.inAppEnabled", is(false)));
    }
}
