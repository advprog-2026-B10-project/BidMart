package id.ac.ui.cs.advprog.bidmart.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerValidationErrorReturnsStandardSchema() throws Exception {
        String payload = """
            {
              "displayName": "",
              "email": "invalid",
              "password": "short",
              "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.details", hasKey("email")));
    }

    @Test
    void duplicateEmailRegisterReturnsConflict() throws Exception {
        User existing = User.builder()
                .email("dup@example.com")
                .password(passwordEncoder.encode("Password!1"))
                .displayName("Existing")
                .role(Role.BUYER)
                .isEnabled(true)
                .build();
        userRepository.save(existing);

        String payload = """
            {
              "displayName": "User",
              "email": "dup@example.com",
              "password": "Password!1",
              "role": "BUYER"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void unverifiedLoginReturnsForbidden() throws Exception {
        User user = User.builder()
                .email("buyer@example.com")
                .password(passwordEncoder.encode("Password!1"))
                .displayName("Buyer")
                .role(Role.BUYER)
                .isEnabled(false)
                .build();
        userRepository.save(user);

        String payload = """
            {
              "email": "buyer@example.com",
              "password": "Password!1"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Please verify your email first"));
    }

    @Test
    void verifyInvalidTokenReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/verify").param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid verification token"));
    }

    @Test
    void usersWithoutTokenReturnsUnauthorizedSchema() throws Exception {
        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void usersWithBuyerTokenReturnsForbiddenSchema() throws Exception {
        User buyer = User.builder()
                .email("buyer2@example.com")
                .password(passwordEncoder.encode("Password!1"))
                .displayName("Buyer2")
                .role(Role.BUYER)
                .isEnabled(true)
                .build();
        userRepository.save(buyer);

        String loginPayload = """
            {
              "email": "buyer2@example.com",
              "password": "Password!1"
            }
            """;

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseBody).get("token").asText();

        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void usersWithAdminTokenReturnsSanitizedUserList() throws Exception {
        User admin = User.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("AdminPass!1"))
                .displayName("Admin")
                .role(Role.ADMIN)
                .isEnabled(true)
                .build();
        userRepository.save(admin);

        User buyer = User.builder()
                .email("listed@example.com")
                .password(passwordEncoder.encode("Password!1"))
                .displayName("Listed User")
                .role(Role.BUYER)
                .isEnabled(true)
                .verificationToken("secret-token")
                .build();
        userRepository.save(buyer);

        String loginPayload = """
            {
              "email": "admin@example.com",
              "password": "AdminPass!1"
            }
            """;

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseBody).get("token").asText();

        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].displayName").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].verificationToken").doesNotExist())
                .andExpect(jsonPath("$[0].mfaSecretSet").doesNotExist());
    }

        @Test
        void profileWithoutTokenReturnsUnauthorizedSchema() throws Exception {
                mockMvc.perform(get("/api/auth/profile"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.message").value("Authentication is required"));
        }

        @Test
        void getProfileWithTokenReturnsCurrentUserProfile() throws Exception {
                User seller = User.builder()
                                .email("seller@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Seller One")
                                .phoneNumber("+628123456789")
                                .role(Role.SELLER)
                                .isEnabled(true)
                                .build();
                userRepository.save(seller);

                String loginPayload = """
                        {
                            "email": "seller@example.com",
                            "password": "Password!1"
                        }
                        """;

                String responseBody = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String token = objectMapper.readTree(responseBody).get("token").asText();

                mockMvc.perform(get("/api/auth/profile")
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("seller@example.com"))
                                .andExpect(jsonPath("$.displayName").value("Seller One"))
                                .andExpect(jsonPath("$.phoneNumber").value("+628123456789"))
                                .andExpect(jsonPath("$.role").value("SELLER"));
        }

        @Test
        void patchProfileUpdatesDisplayNameAndPhoneNumber() throws Exception {
                User buyer = User.builder()
                                .email("profile@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Old Name")
                                .phoneNumber("0812345678")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .build();
                userRepository.save(buyer);

                String loginPayload = """
                        {
                            "email": "profile@example.com",
                            "password": "Password!1"
                        }
                        """;

                String responseBody = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String token = objectMapper.readTree(responseBody).get("token").asText();

                String updatePayload = """
                        {
                            "displayName": "New Name",
                            "phoneNumber": "+628888777666"
                        }
                        """;

                mockMvc.perform(patch("/api/auth/profile")
                                                .header("Authorization", "Bearer " + token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(updatePayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.displayName").value("New Name"))
                                .andExpect(jsonPath("$.phoneNumber").value("+628888777666"));

                User updated = userRepository.findByEmail("profile@example.com").orElseThrow();
                org.junit.jupiter.api.Assertions.assertEquals("New Name", updated.getDisplayName());
                org.junit.jupiter.api.Assertions.assertEquals("+628888777666", updated.getPhoneNumber());
        }

        @Test
        void patchProfileInvalidPhoneReturnsValidationSchema() throws Exception {
                User buyer = User.builder()
                                .email("invalidphone@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Buyer")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .build();
                userRepository.save(buyer);

                String loginPayload = """
                        {
                            "email": "invalidphone@example.com",
                            "password": "Password!1"
                        }
                        """;

                String responseBody = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String token = objectMapper.readTree(responseBody).get("token").asText();

                String updatePayload = """
                        {
                            "phoneNumber": "abc-123"
                        }
                        """;

                mockMvc.perform(patch("/api/auth/profile")
                                                .header("Authorization", "Bearer " + token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(updatePayload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.details", hasKey("phoneNumber")));
        }

        @Test
        void loginWithMfaEnabledReturnsMfaRequiredWithoutAccessToken() throws Exception {
                User buyer = User.builder()
                                .email("mfauser@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Mfa User")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .mfaEnabled(true)
                                .build();
                userRepository.save(buyer);

                String loginPayload = """
                        {
                            "email": "mfauser@example.com",
                            "password": "Password!1"
                        }
                        """;

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.mfaRequired").value(true))
                        .andExpect(jsonPath("$.message").value("MFA verification is required"))
                        .andExpect(jsonPath("$.token").doesNotExist())
                        .andExpect(jsonPath("$.refreshToken").doesNotExist());
        }

        @Test
        void toggleMfaWithAuthenticatedUserUpdatesMfaFlag() throws Exception {
                User buyer = User.builder()
                                .email("togglemfa@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Toggle Mfa")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .mfaEnabled(false)
                                .build();
                userRepository.save(buyer);

                String loginPayload = """
                        {
                            "email": "togglemfa@example.com",
                            "password": "Password!1"
                        }
                        """;

                String responseBody = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String token = objectMapper.readTree(responseBody).get("token").asText();

                String togglePayload = """
                        {
                            "enabled": true
                        }
                        """;

                mockMvc.perform(post("/api/auth/mfa/toggle")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(togglePayload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.mfaEnabled").value(true))
                        .andExpect(jsonPath("$.message").value("MFA enabled successfully"));

                User updated = userRepository.findByEmail("togglemfa@example.com").orElseThrow();
                org.junit.jupiter.api.Assertions.assertTrue(updated.isMfaEnabled());
        }
}
