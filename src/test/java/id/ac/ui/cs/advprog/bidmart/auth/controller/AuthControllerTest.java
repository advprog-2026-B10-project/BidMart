package id.ac.ui.cs.advprog.bidmart.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.auth.dto.*;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.exception.AuthException;
import id.ac.ui.cs.advprog.bidmart.auth.service.AdminPermissionService;
import id.ac.ui.cs.advprog.bidmart.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AdminPermissionService adminPermissionService;

    @Test
    void registerValidationErrorReturnsStandardSchema() throws Exception {
        String payload = """
            {"displayName": "", "email": "invalid", "password": "short", "role": "ADMIN"}
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
        doThrow(new AuthException(HttpStatus.CONFLICT, "Email already registered"))
                .when(authService).register(any(RegisterRequest.class));

        String payload = """
            {"displayName": "User", "email": "dup@example.com", "password": "Password!1", "role": "BUYER"}
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
        doThrow(new AuthException(HttpStatus.FORBIDDEN, "Please verify your email first"))
                .when(authService).login(any(LoginRequest.class));

        String payload = """
            {"email": "buyer@example.com", "password": "Password!1"}
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
        doThrow(new AuthException(HttpStatus.BAD_REQUEST, "Invalid verification token"))
                .when(authService).verifyUser("invalid-token");

        mockMvc.perform(get("/api/auth/verify").param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid verification token"));
    }

    @Test
    void usersWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void usersWithAdminTokenReturnsSanitizedUserList() throws Exception {
        AdminUserResponse user = AdminUserResponse.builder()
                .id(1L).email("a@b.com").displayName("A").role(Role.ADMIN).activeSessions(2)
                .roleGroups(Set.of()).build();
        when(authService.getAdminUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].displayName").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminUserListIncludesRoleGroups() throws Exception {
        AdminUserResponse user = AdminUserResponse.builder()
                .id(1L).email("a@b.com").displayName("A").role(Role.ADMIN).activeSessions(0)
                .roleGroups(Set.of()).build();
        when(authService.getAdminUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleGroups").isArray());
    }

    @Test
    void profileWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SELLER", "profile:view"})
    void getProfileWithTokenReturnsCurrentUserProfile() throws Exception {
        ProfileResponse profile = ProfileResponse.builder()
                .email("seller@example.com").displayName("Seller One")
                .phoneNumber("+628123456789").role(Role.SELLER).build();
        when(authService.getProfile("user")).thenReturn(profile);

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("seller@example.com"))
                .andExpect(jsonPath("$.displayName").value("Seller One"))
                .andExpect(jsonPath("$.phoneNumber").value("+628123456789"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void loginWithMfaEnabledReturnsMfaRequired() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                AuthResponse.builder().mfaRequired(true).message("MFA verification is required").build()
        );

        String payload = """
            {"email": "mfauser@example.com", "password": "Password!1"}
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.message").value("MFA verification is required"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SELLER", "profile:edit"})
    void patchProfileInvalidPhoneReturnsValidationSchema() throws Exception {
        String updatePayload = """
            {"phoneNumber": "abc-123"}
            """;

        mockMvc.perform(patch("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", hasKey("phoneNumber")));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanListPermissions() throws Exception {
        when(adminPermissionService.getAllPermissions())
                .thenReturn(List.of(new PermissionResponse(1L, "role:manage")));

        mockMvc.perform(get("/api/auth/admin/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanCreatePermission() throws Exception {
        when(adminPermissionService.createPermission(any(CreatePermissionRequest.class)))
                .thenReturn(new PermissionResponse(1L, "test:perm"));

        String body = "{\"name\": \"test:perm\"}";

        mockMvc.perform(post("/api/auth/admin/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("test:perm"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/auth/admin/permissions/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanListRoleGroups() throws Exception {
        when(adminPermissionService.getAllRoleGroups()).thenReturn(List.of(
                RoleGroupResponse.builder().id(1L).name("ADMIN").permissions(Set.of()).build()
        ));

        mockMvc.perform(get("/api/auth/admin/role-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].permissions").isArray());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanCreateRoleGroup() throws Exception {
        RoleGroupResponse rg = RoleGroupResponse.builder().id(1L).name("TEST_ROLE").permissions(Set.of()).build();
        when(adminPermissionService.createRoleGroup(any(CreateRoleGroupRequest.class))).thenReturn(rg);

        String body = "{\"name\": \"TEST_ROLE\"}";

        mockMvc.perform(post("/api/auth/admin/role-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("TEST_ROLE"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanUpdateRoleGroup() throws Exception {
        RoleGroupResponse rg = RoleGroupResponse.builder().id(1L).name("UPDATED").permissions(Set.of()).build();
        when(adminPermissionService.updateRoleGroup(eq(1L), any(UpdateRoleGroupRequest.class))).thenReturn(rg);

        String body = "{\"name\": \"UPDATED\", \"permissionIds\": []}";

        mockMvc.perform(put("/api/auth/admin/role-groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("UPDATED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanDeleteRoleGroup() throws Exception {
        mockMvc.perform(delete("/api/auth/admin/role-groups/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminCanUpdateUserRoles() throws Exception {
        String body = "{\"roleGroupIds\": [1]}";

        mockMvc.perform(put("/api/auth/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_BUYER")
    void nonAdminCannotManagePermissions() throws Exception {
        mockMvc.perform(get("/api/auth/admin/permissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getMySessionsReturnsList() throws Exception {
        when(authService.getUserSessions("user")).thenReturn(List.of(
                new UserSessionResponse(1L, Instant.now(), Instant.now().plusSeconds(3600))
        ));

        mockMvc.perform(get("/api/auth/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @WithMockUser
    void revokeSessionReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/auth/sessions/1"))
                .andExpect(status().isOk());
    }
}
