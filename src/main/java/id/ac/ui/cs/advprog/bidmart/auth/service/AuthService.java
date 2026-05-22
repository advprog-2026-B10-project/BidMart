package id.ac.ui.cs.advprog.bidmart.auth.service;

import id.ac.ui.cs.advprog.bidmart.auth.dto.*;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RefreshToken;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.exception.AuthException;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import id.ac.ui.cs.advprog.bidmart.auth.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RoleGroupRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService; 
    private final MfaTotpService mfaTotpService;
    private final EmailService emailService;
    private final RoleGroupRepository roleGroupRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.auth.max-concurrent-sessions:3}")
    private int maxConcurrentSessions;

    @Value("${app.auth.session-policy:revoke_oldest}")
    private String sessionPolicy;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(HttpStatus.CONFLICT, "Email already registered");
        }

        Role assignedRole;
        try {
            assignedRole = Role.valueOf(request.getRole().toUpperCase());
            if (assignedRole == Role.ADMIN) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "Cannot register as Admin");
            }
        } catch (IllegalArgumentException e) {
            assignedRole = Role.BUYER; 
        }

        String token = UUID.randomUUID().toString();

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName())
            .role(assignedRole)
            .verificationToken(token)
            .isEnabled(false) 
            .build();

        roleGroupRepository.findByName(assignedRole.name()).ifPresent(rg ->
                user.setRoleGroups(java.util.Set.of(rg)));

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), token);
        
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isEnabled()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Account is already verified");
        }

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Please verify your email first");
        }

        if (user.isMfaEnabled()) {
            if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
                throw new AuthException(HttpStatus.CONFLICT, "MFA is enabled but not configured for this account");
            }

            return AuthResponse.builder()
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .mfaRequired(true)
                    .message("MFA verification is required")
                    .mfaChallengeToken(jwtService.generateMfaChallengeToken(user))
                    .build();
        }

        return issueSessionTokens(user);
    }

    public AuthResponse refreshTokens(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }

        if (Instant.now().isAfter(refreshToken.getExpiresAt())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        if (!jwtService.isRefreshTokenValid(refreshTokenValue)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        User user = userRepository.findByEmail(refreshToken.getEmail())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .email(user.getEmail())
                .role(user.getRole().name())
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public AuthResponse verifyMfa(MfaVerifyRequest request) {
        if (!jwtService.isMfaChallengeTokenValid(request.getChallengeToken())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid MFA challenge token");
        }

        String email = jwtService.extractEmail(request.getChallengeToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new AuthException(HttpStatus.CONFLICT, "MFA is not enabled for this user");
        }

        if (!mfaTotpService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        return issueSessionTokens(user);
    }

    public MfaStatusResponse toggleMfa(String email, boolean enabled) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        user.setMfaEnabled(enabled);
        if (enabled) {
            if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
                user.setMfaSecret(mfaTotpService.generateSecret());
            }
            user.setMfaSecretSet(true);
        } else {
            user.setMfaSecret(null);
            user.setMfaSecretSet(false);
        }
        userRepository.save(user);

        return MfaStatusResponse.builder()
                .mfaEnabled(user.isMfaEnabled())
                .message(enabled ? "MFA enabled successfully" : "MFA disabled successfully")
                .secret(enabled ? user.getMfaSecret() : null)
                .otpauthUri(enabled ? mfaTotpService.buildOtpAuthUri(user.getEmail(), user.getMfaSecret()) : null)
                .build();
    }

    public List<AdminUserResponse> getAdminUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> AdminUserResponse.fromUser(
                        user,
                        refreshTokenRepository.countActiveSessionsByEmail(user.getEmail())
                ))
                .toList();
    }

    @Transactional
    public AdminSessionRevokeResponse revokeUserSessions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        int revoked = refreshTokenRepository.revokeActiveSessionsByEmail(user.getEmail());
        return AdminSessionRevokeResponse.builder()
                .userId(userId)
                .revokedSessions(revoked)
                .message("Revoked active sessions successfully")
                .build();
    }

    @Transactional
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isEnabled()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "User is already disabled");
        }

        user.setEnabled(false);
        userRepository.save(user);

        refreshTokenRepository.revokeActiveSessionsByEmail(user.getEmail());
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long userId, String roleValue, String actorEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        Role newRole;
        try {
            newRole = Role.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Invalid role value");
        }

        if (user.getEmail().equalsIgnoreCase(actorEmail) && newRole != Role.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Admin cannot demote themselves");
        }

        user.setRole(newRole);
        userRepository.save(user);

        return AdminUserResponse.fromUser(user, refreshTokenRepository.countActiveSessionsByEmail(user.getEmail()));
    }

    @Transactional
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        userRepository.save(user);

        refreshTokenRepository.deleteByEmail(user.getEmail());
    }

    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid verification token"));
        
        user.setEnabled(true);
        user.setVerificationToken(null); 
        userRepository.save(user);
    }

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
        return ProfileResponse.fromUser(user);
    }

    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName().trim());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getShippingAddress() != null) {
            user.setShippingAddress(request.getShippingAddress().trim());
        }

        userRepository.save(user);
        return ProfileResponse.fromUser(user);
    }

    private AuthResponse issueSessionTokens(User user) {
        enforceConcurrentSessionLimit(user.getEmail());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .email(user.getEmail())
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .email(user.getEmail())
                .role(user.getRole().name())
                .mfaRequired(false)
                .build();
    }

    private void enforceConcurrentSessionLimit(String email) {
        List<RefreshToken> activeSessions = refreshTokenRepository.findActiveSessionsByEmail(email);
        int overflow = activeSessions.size() - maxConcurrentSessions + 1;

        if (overflow <= 0) {
            return;
        }

        if ("reject_new".equals(sessionPolicy)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum concurrent sessions (" + maxConcurrentSessions + ") reached. Please log out from another device first.");
        }

        for (int i = 0; i < overflow && i < activeSessions.size(); i++) {
            RefreshToken token = activeSessions.get(i);
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
    }
}