package id.ac.ui.cs.advprog.bidmart.auth.service;

import id.ac.ui.cs.advprog.bidmart.auth.dto.*;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RefreshToken;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.exception.AuthException;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RefreshTokenRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RoleGroupRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private MfaTotpService mfaTotpService;
    @Mock private EmailService emailService;
    @Mock private RoleGroupRepository roleGroupRepository;

    private AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User buyerUser;
    private User mfaUser;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService(userRepository, refreshTokenRepository, jwtService,
                mfaTotpService, emailService, roleGroupRepository);

        setField("maxConcurrentSessions", 3);
        setField("sessionPolicy", "revoke_oldest");

        buyerUser = User.builder().id(1L).email("buyer@test.com")
                .password(encoder.encode("Password!1")).displayName("Buyer")
                .role(Role.BUYER).isEnabled(true).build();

        mfaUser = User.builder().id(4L).email("mfa@test.com")
                .password(encoder.encode("Password!1")).displayName("Mfa")
                .role(Role.BUYER).isEnabled(true).mfaEnabled(true)
                .mfaSecret("JBSWY3DPEHPK3PXP").mfaSecretSet(true).build();
    }

    private void setField(String name, Object value) throws Exception {
        Field field = AuthService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(authService, value);
    }

    private RegisterRequest regReq(String email, String role) {
        RegisterRequest r = new RegisterRequest();
        r.setDisplayName("User");
        r.setEmail(email);
        r.setPassword("Password!1");
        r.setRole(role);
        return r;
    }

    private LoginRequest loginReq(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    @Test
    void register_Success() {
        RoleGroup rg = RoleGroup.builder().id(1L).name("BUYER").permissions(Set.of()).build();
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleGroupRepository.findByName("BUYER")).thenReturn(Optional.of(rg));

        authService.register(regReq("new@test.com", "BUYER"));

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("new@test.com", saved.getEmail());
        assertFalse(saved.isEnabled());
        assertNotNull(saved.getVerificationToken());
        assertEquals(Role.BUYER, saved.getRole());
        assertTrue(encoder.matches("Password!1", saved.getPassword()));
        verify(emailService).sendVerificationEmail(eq("new@test.com"), anyString());
    }

    @Test
    void register_DuplicateEmail_ThrowsConflict() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);
        AuthException ex = assertThrows(AuthException.class,
                () -> authService.register(regReq("dup@test.com", "BUYER")));
        assertEquals("Email already registered", ex.getMessage());
    }

    @Test
    void login_ValidCredentials_NoMfa_ReturnsTokens() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));
        when(jwtService.generateToken(buyerUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(buyerUser)).thenReturn("refresh-token");
        when(refreshTokenRepository.findActiveSessionsByEmail("buyer@test.com")).thenReturn(List.of());

        AuthResponse response = authService.login(loginReq("buyer@test.com", "Password!1"));

        assertNotNull(response);
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertFalse(response.isMfaRequired());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_InvalidCredentials_ThrowsUnauthorized() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));
        assertThrows(AuthException.class,
                () -> authService.login(loginReq("buyer@test.com", "WrongPassword!1")));
    }

    @Test
    void login_UnverifiedEmail_ThrowsForbidden() {
        buyerUser.setEnabled(false);
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));
        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login(loginReq("buyer@test.com", "Password!1")));
        assertEquals("Please verify your email first", ex.getMessage());
    }

    @Test
    void login_MfaEnabled_ReturnsMfaChallenge() {
        when(userRepository.findByEmail("mfa@test.com")).thenReturn(Optional.of(mfaUser));
        when(jwtService.generateMfaChallengeToken(mfaUser)).thenReturn("mfa-challenge");

        AuthResponse response = authService.login(loginReq("mfa@test.com", "Password!1"));

        assertTrue(response.isMfaRequired());
        assertEquals("mfa-challenge", response.getMfaChallengeToken());
        assertNull(response.getToken());
        assertNull(response.getRefreshToken());
    }

    @Test
    void verifyMfa_ValidCode_ReturnsTokens() {
        when(jwtService.isMfaChallengeTokenValid("valid-challenge")).thenReturn(true);
        when(jwtService.extractEmail("valid-challenge")).thenReturn("mfa@test.com");
        when(userRepository.findByEmail("mfa@test.com")).thenReturn(Optional.of(mfaUser));
        when(mfaTotpService.verifyCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);
        when(jwtService.generateToken(mfaUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(mfaUser)).thenReturn("refresh-token");
        when(refreshTokenRepository.findActiveSessionsByEmail("mfa@test.com")).thenReturn(List.of());

        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setChallengeToken("valid-challenge");
        request.setCode("123456");

        AuthResponse response = authService.verifyMfa(request);

        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertFalse(response.isMfaRequired());
    }

    @Test
    void verifyMfa_InvalidChallengeToken_ThrowsUnauthorized() {
        when(jwtService.isMfaChallengeTokenValid("bad-challenge")).thenReturn(false);

        MfaVerifyRequest request = new MfaVerifyRequest();
        request.setChallengeToken("bad-challenge");
        request.setCode("123456");

        assertThrows(AuthException.class, () -> authService.verifyMfa(request));
    }

    @Test
    void toggleMfa_Enable_GeneratesSecret() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));
        when(mfaTotpService.generateSecret()).thenReturn("NEW-SECRET");
        when(mfaTotpService.buildOtpAuthUri(eq("buyer@test.com"), anyString())).thenReturn("otpauth://...");

        MfaStatusResponse response = authService.toggleMfa("buyer@test.com", true);

        assertTrue(response.isMfaEnabled());
        assertNotNull(response.getSecret());
        assertNotNull(response.getOtpauthUri());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void toggleMfa_Disable_ClearsSecret() {
        when(userRepository.findByEmail("mfa@test.com")).thenReturn(Optional.of(mfaUser));

        MfaStatusResponse response = authService.toggleMfa("mfa@test.com", false);

        assertFalse(response.isMfaEnabled());
        assertNull(response.getSecret());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getProfile_ReturnsUserData() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));

        ProfileResponse response = authService.getProfile("buyer@test.com");

        assertEquals("buyer@test.com", response.getEmail());
        assertEquals("Buyer", response.getDisplayName());
        assertEquals(Role.BUYER, response.getRole());
    }

    @Test
    void updateProfile_UpdatesFields() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");
        request.setPhoneNumber("+628111222333");

        ProfileResponse response = authService.updateProfile("buyer@test.com", request);

        assertEquals("New Name", response.getDisplayName());
        assertEquals("+628111222333", response.getPhoneNumber());
    }

    @Test
    void logout_DeletesTokens() {
        authService.logout("buyer@test.com");
        verify(refreshTokenRepository).deleteByEmail("buyer@test.com");
    }

    @Test
    void verifyUser_ValidToken_EnablesUser() {
        buyerUser.setVerificationToken("my-token");
        when(userRepository.findByVerificationToken("my-token")).thenReturn(Optional.of(buyerUser));

        authService.verifyUser("my-token");

        assertTrue(buyerUser.isEnabled());
        assertNull(buyerUser.getVerificationToken());
        verify(userRepository).save(buyerUser);
    }

    @Test
    void verifyUser_InvalidToken_ThrowsBadRequest() {
        when(userRepository.findByVerificationToken("bad-token")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> authService.verifyUser("bad-token"));
        assertEquals("Invalid verification token", ex.getMessage());
    }

    @Test
    void forgotPassword_SendsEmail() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("buyer@test.com");
        authService.forgotPassword(request);

        verify(emailService).sendPasswordResetEmail(eq("buyer@test.com"), anyString());
    }

    @Test
    void resetPassword_ValidToken_UpdatesPassword() {
        buyerUser.setPasswordResetToken("reset-token");
        when(userRepository.findByPasswordResetToken("reset-token")).thenReturn(Optional.of(buyerUser));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("NewPass!1");
        authService.resetPassword(request);

        assertTrue(encoder.matches("NewPass!1", buyerUser.getPassword()));
        assertNull(buyerUser.getPasswordResetToken());
        verify(userRepository).save(buyerUser);
        verify(refreshTokenRepository).deleteByEmail("buyer@test.com");
    }

    @Test
    void getUserSessions_ReturnsActiveSessions() {
        RefreshToken rt = RefreshToken.builder().id(1L).email("buyer@test.com")
                .createdAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
        when(refreshTokenRepository.findActiveSessionsByEmail("buyer@test.com")).thenReturn(List.of(rt));

        List<UserSessionResponse> sessions = authService.getUserSessions("buyer@test.com");

        assertEquals(1, sessions.size());
        assertEquals(1L, sessions.getFirst().getId());
    }

    @Test
    void revokeSession_Own_Revokes() {
        RefreshToken rt = RefreshToken.builder().id(1L).email("buyer@test.com").build();
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(rt));

        authService.revokeSession(1L, "buyer@test.com");

        assertTrue(rt.isRevoked());
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    void revokeSession_Others_ThrowsForbidden() {
        RefreshToken rt = RefreshToken.builder().id(1L).email("other@test.com").build();
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(rt));

        assertThrows(AuthException.class, () -> authService.revokeSession(1L, "buyer@test.com"));
    }

    @Test
    void resendVerificationEmail_Resends() {
        buyerUser.setEnabled(false);
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));

        authService.resendVerificationEmail("buyer@test.com");

        verify(emailService).sendVerificationEmail(eq("buyer@test.com"), anyString());
    }

    @Test
    void resendVerificationEmail_AlreadyVerified_Throws() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));

        assertThrows(AuthException.class, () -> authService.resendVerificationEmail("buyer@test.com"));
    }

    @Test
    void getAdminUsers_ReturnsAllWithCounts() {
        User adminUser = User.builder().id(2L).email("admin@test.com")
                .password("encoded").displayName("Admin").role(Role.ADMIN).isEnabled(true).build();
        when(userRepository.findAll()).thenReturn(List.of(buyerUser, adminUser));
        when(refreshTokenRepository.countActiveSessionsByEmail("buyer@test.com")).thenReturn(2L);
        when(refreshTokenRepository.countActiveSessionsByEmail("admin@test.com")).thenReturn(1L);

        List<AdminUserResponse> users = authService.getAdminUsers();

        assertEquals(2, users.size());
        assertEquals(2L, users.get(0).getActiveSessions());
        assertNotNull(users.get(0).getRoleGroups());
    }

    @Test
    void disableUser_DisablesAndRevokes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser));

        authService.disableUser(1L);

        assertFalse(buyerUser.isEnabled());
        verify(userRepository).save(buyerUser);
        verify(refreshTokenRepository).revokeActiveSessionsByEmail("buyer@test.com");
    }

    @Test
    void disableUser_AlreadyDisabled_Throws() {
        buyerUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser));

        assertThrows(AuthException.class, () -> authService.disableUser(1L));
    }

    @Test
    void revokeUserSessions_RevokesActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser));
        when(refreshTokenRepository.revokeActiveSessionsByEmail("buyer@test.com")).thenReturn(2);

        AdminSessionRevokeResponse response = authService.revokeUserSessions(1L);

        assertEquals(2, response.getRevokedSessions());
    }

    @Test
    void enforceConcurrentSessionLimit_RevokesOldest() throws Exception {
        setField("maxConcurrentSessions", 1);
        RefreshToken old = RefreshToken.builder().id(1L).revoked(false).build();
        when(refreshTokenRepository.findActiveSessionsByEmail("buyer@test.com")).thenReturn(List.of(old));
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));
        when(jwtService.generateToken(buyerUser)).thenReturn("tok");
        when(jwtService.generateRefreshToken(buyerUser)).thenReturn("rtok");

        authService.login(loginReq("buyer@test.com", "Password!1"));

        assertTrue(old.isRevoked());
    }

    @Test
    void refreshTokens_Valid_ReturnsNewAccessToken() {
        String refreshValue = "valid-refresh";
        RefreshToken rt = RefreshToken.builder().token(refreshValue).email("buyer@test.com")
                .expiresAt(Instant.now().plusSeconds(3600)).revoked(false).build();
        when(refreshTokenRepository.findByToken(refreshValue)).thenReturn(Optional.of(rt));
        when(jwtService.isRefreshTokenValid(refreshValue)).thenReturn(true);
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyerUser));
        when(jwtService.generateToken(buyerUser)).thenReturn("new-access");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshValue);

        AuthResponse response = authService.refreshTokens(request.getRefreshToken());

        assertEquals("new-access", response.getToken());
    }
}
