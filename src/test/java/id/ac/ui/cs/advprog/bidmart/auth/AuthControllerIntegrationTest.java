package id.ac.ui.cs.advprog.bidmart.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RefreshTokenRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RoleGroupRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.auth.service.MfaTotpService;
import id.ac.ui.cs.advprog.bidmart.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private RoleGroupRepository roleGroupRepository;

        @Autowired
        private MfaTotpService mfaTotpService;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private User saveWithRoleGroup(User user, String roleGroupName) {
        roleGroupRepository.findByName(roleGroupName).ifPresent(rg -> user.setRoleGroups(java.util.Set.of(rg)));
        return userRepository.save(user);
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
        saveWithRoleGroup(seller, "SELLER");

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
                saveWithRoleGroup(buyer, "BUYER");

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
                saveWithRoleGroup(buyer, "BUYER");

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
                                .mfaSecret(mfaTotpService.generateSecret())
                                .mfaSecretSet(true)
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
                        .andExpect(jsonPath("$.message").value("MFA enabled successfully"))
                        .andExpect(jsonPath("$.secret").exists())
                        .andExpect(jsonPath("$.otpauthUri").exists());

                User updated = userRepository.findByEmail("togglemfa@example.com").orElseThrow();
                org.junit.jupiter.api.Assertions.assertTrue(updated.isMfaEnabled());
                org.junit.jupiter.api.Assertions.assertNotNull(updated.getMfaSecret());
        }

        @Test
        void verifyMfaWithValidCodeReturnsTokens() throws Exception {
                User buyer = User.builder()
                                .email("mfaverify@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Mfa Verify")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .mfaEnabled(true)
                                .mfaSecret(mfaTotpService.generateSecret())
                                .mfaSecretSet(true)
                                .build();
                userRepository.save(buyer);

                String loginPayload = """
                        {
                            "email": "mfaverify@example.com",
                            "password": "Password!1"
                        }
                        """;

                String loginBody = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mfaRequired").value(true))
                                .andExpect(jsonPath("$.mfaChallengeToken").exists())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String challengeToken = objectMapper.readTree(loginBody).get("mfaChallengeToken").asText();
                String code = mfaTotpService.generateCurrentCode(buyer.getMfaSecret());

                String verifyPayload = """
                        {
                            "challengeToken": "%s",
                            "code": "%s"
                        }
                        """.formatted(challengeToken, code);

                mockMvc.perform(post("/api/auth/mfa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verifyPayload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").exists())
                        .andExpect(jsonPath("$.refreshToken").exists())
                        .andExpect(jsonPath("$.mfaRequired").value(false));
        }

        @Test
        void loginCapsActiveConcurrentSessionsToConfiguredLimit() throws Exception {
                User user = User.builder()
                                .email("sessions@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Session User")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .build();
                userRepository.save(user);

                String loginPayload = """
                        {
                            "email": "sessions@example.com",
                            "password": "Password!1"
                        }
                        """;

                for (int i = 0; i < 4; i++) {
                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").exists())
                                .andExpect(jsonPath("$.refreshToken").exists());
                }

                long activeSessions = refreshTokenRepository.countActiveSessionsByEmail("sessions@example.com");
                org.junit.jupiter.api.Assertions.assertEquals(3, activeSessions);
        }

        @Test
        void adminCanRevokeSessionsAndUpdateRole() throws Exception {
                User admin = User.builder()
                                .email("admincontrol@example.com")
                                .password(passwordEncoder.encode("AdminPass!1"))
                                .displayName("Admin")
                                .role(Role.ADMIN)
                                .isEnabled(true)
                                .build();
                userRepository.save(admin);

                User target = User.builder()
                                .email("target@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Target")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .build();
                target = userRepository.save(target);

                String targetLogin = """
                        {
                            "email": "target@example.com",
                            "password": "Password!1"
                        }
                        """;
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(targetLogin))
                        .andExpect(status().isOk());

                String adminLogin = """
                        {
                            "email": "admincontrol@example.com",
                            "password": "AdminPass!1"
                        }
                        """;
                String adminLoginBody = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(adminLogin))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String adminToken = objectMapper.readTree(adminLoginBody).get("token").asText();

                mockMvc.perform(post("/api/auth/admin/users/" + target.getId() + "/sessions/revoke")
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.revokedSessions").value(1));

                String rolePayload = """
                        {
                            "role": "SELLER"
                        }
                        """;

                mockMvc.perform(patch("/api/auth/admin/users/" + target.getId() + "/role")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rolePayload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.role").value("SELLER"));

                User updatedTarget = userRepository.findById(target.getId()).orElseThrow();
                org.junit.jupiter.api.Assertions.assertEquals(Role.SELLER, updatedTarget.getRole());
        }

        @Test
        void adminUserListIncludesRoleGroups() throws Exception {
                User admin = User.builder()
                        .email("roleadmin@example.com")
                        .password(passwordEncoder.encode("AdminPass!1"))
                        .displayName("Role Admin")
                        .role(Role.ADMIN)
                        .isEnabled(true)
                        .build();
                userRepository.save(admin);

                String token = loginAs(admin.getEmail(), "AdminPass!1");

                mockMvc.perform(get("/api/auth/users")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].roleGroups").isArray());
        }

        @Test
        void adminCanListPermissions() throws Exception {
                User admin = User.builder()
                        .email("permlist@example.com")
                        .password(passwordEncoder.encode("AdminPass!1"))
                        .displayName("Perm List Admin")
                        .role(Role.ADMIN)
                        .isEnabled(true)
                        .build();
                userRepository.save(admin);

                String token = loginAs(admin.getEmail(), "AdminPass!1");

                mockMvc.perform(get("/api/auth/admin/permissions")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[*].id").exists())
                        .andExpect(jsonPath("$[*].name").exists());
        }

        @Test
        void adminCanCreateAndDeletePermission() throws Exception {
                User admin = User.builder()
                        .email("permcrud@example.com")
                        .password(passwordEncoder.encode("AdminPass!1"))
                        .displayName("Perm CRUD Admin")
                        .role(Role.ADMIN)
                        .isEnabled(true)
                        .build();
                userRepository.save(admin);

                String token = loginAs(admin.getEmail(), "AdminPass!1");

                String createBody = """
                        { "name": "test:perm" }
                        """;

                String responseBody = mockMvc.perform(post("/api/auth/admin/permissions")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.name").value("test:perm"))
                        .andReturn().getResponse().getContentAsString();

                Long permId = objectMapper.readTree(responseBody).get("id").asLong();

                mockMvc.perform(delete("/api/auth/admin/permissions/" + permId)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk());
        }

        @Test
        void adminCanListRoleGroups() throws Exception {
                User admin = User.builder()
                        .email("rglist@example.com")
                        .password(passwordEncoder.encode("AdminPass!1"))
                        .displayName("RG List Admin")
                        .role(Role.ADMIN)
                        .isEnabled(true)
                        .build();
                userRepository.save(admin);

                String token = loginAs(admin.getEmail(), "AdminPass!1");

                mockMvc.perform(get("/api/auth/admin/role-groups")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[*].id").exists())
                        .andExpect(jsonPath("$[*].name").exists())
                        .andExpect(jsonPath("$[*].permissions").isArray());
        }

        @Test
        void adminCanCreateUpdateAndDeleteRoleGroup() throws Exception {
                User admin = User.builder()
                        .email("rgcrud@example.com")
                        .password(passwordEncoder.encode("AdminPass!1"))
                        .displayName("RG CRUD Admin")
                        .role(Role.ADMIN)
                        .isEnabled(true)
                        .build();
                userRepository.save(admin);

                String token = loginAs(admin.getEmail(), "AdminPass!1");

                String createBody = """
                        { "name": "TEST_ROLE" }
                        """;

                String responseBody = mockMvc.perform(post("/api/auth/admin/role-groups")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.name").value("TEST_ROLE"))
                        .andReturn().getResponse().getContentAsString();

                Long rgId = objectMapper.readTree(responseBody).get("id").asLong();

                String updateBody = """
                        { "name": "TEST_ROLE_UPDATED", "permissionIds": [] }
                        """;

                mockMvc.perform(put("/api/auth/admin/role-groups/" + rgId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value("TEST_ROLE_UPDATED"));

                mockMvc.perform(delete("/api/auth/admin/role-groups/" + rgId)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk());
        }

        @Test
        void adminCanUpdateUserRoles() throws Exception {
                User admin = User.builder()
                        .email("userroleadmin@example.com")
                        .password(passwordEncoder.encode("AdminPass!1"))
                        .displayName("User Role Admin")
                        .role(Role.ADMIN)
                        .isEnabled(true)
                        .build();
                userRepository.save(admin);

                User target = User.builder()
                        .email("userroletarget@example.com")
                        .password(passwordEncoder.encode("Password!1"))
                        .displayName("Target User")
                        .role(Role.BUYER)
                        .isEnabled(true)
                        .build();
                target = userRepository.save(target);

                String token = loginAs(admin.getEmail(), "AdminPass!1");

                RoleGroup buyerRg = roleGroupRepository.findByName("BUYER").orElseThrow();

                String rolesBody = """
                        { "roleGroupIds": [%d] }
                        """.formatted(buyerRg.getId());

                mockMvc.perform(put("/api/auth/admin/users/" + target.getId() + "/roles")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rolesBody))
                        .andExpect(status().isOk());
        }

        @Test
        void nonAdminCannotManagePermissions() throws Exception {
                User buyer = User.builder()
                        .email("buyernoperm@example.com")
                        .password(passwordEncoder.encode("Password!1"))
                        .displayName("Buyer No Perm")
                        .role(Role.BUYER)
                        .isEnabled(true)
                        .build();
                userRepository.save(buyer);

                String token = loginAs(buyer.getEmail(), "Password!1");

                mockMvc.perform(get("/api/auth/admin/permissions")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isForbidden());
        }

        private String loginAs(String email, String password) throws Exception {
                String loginPayload = """
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, password);

                String responseBody = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();

                return objectMapper.readTree(responseBody).get("token").asText();
        }

        @Nested
        @SpringBootTest
        @AutoConfigureMockMvc
        @TestPropertySource(properties = "app.auth.session-policy=reject_new")
        class RejectSessionPolicyTest {

                @Autowired
                private MockMvc mockMvc;

                @Autowired
                private UserRepository userRepository;

                @Autowired
                private PasswordEncoder passwordEncoder;

                @Autowired
                private RefreshTokenRepository refreshTokenRepository;

                @Autowired
                @MockBean
                private EmailService emailServiceMock;

                @Test
                void loginWithRejectPolicyRefusesExcessSessions() throws Exception {
                        User user = User.builder()
                                .email("reject-user@example.com")
                                .password(passwordEncoder.encode("Password!1"))
                                .displayName("Reject User")
                                .role(Role.BUYER)
                                .isEnabled(true)
                                .build();
                        userRepository.save(user);

                        String loginPayload = """
                                {
                                    "email": "reject-user@example.com",
                                    "password": "Password!1"
                                }
                                """;

                        for (int i = 0; i < 3; i++) {
                                mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(loginPayload))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.token").exists())
                                        .andExpect(jsonPath("$.refreshToken").exists());
                        }

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginPayload))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.message").value(
                                        "Maximum concurrent sessions (3) reached. Please log out from another device first."));

                        long activeSessions = refreshTokenRepository.countActiveSessionsByEmail("reject-user@example.com");
                        org.junit.jupiter.api.Assertions.assertEquals(3, activeSessions);
                }
        }
}
