package id.ac.ui.cs.advprog.bidmart.auth.service;

import id.ac.ui.cs.advprog.bidmart.auth.dto.*;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RefreshToken;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.exception.AuthException;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService; 
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), token);
        
    }

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
            return AuthResponse.builder()
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .mfaRequired(true)
                    .message("MFA verification is required")
                    .build();
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save refresh token to database
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

    public AuthResponse refreshTokens(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }

        if (Instant.now().isAfter(refreshToken.getExpiresAt())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        if (!jwtService.isTokenValid(refreshTokenValue)) {
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

    public MfaStatusResponse toggleMfa(String email, boolean enabled) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        user.setMfaEnabled(enabled);
        if (enabled) {
            user.setMfaSecretSet(true);
        }
        userRepository.save(user);

        return MfaStatusResponse.builder()
                .mfaEnabled(user.isMfaEnabled())
                .message(enabled ? "MFA enabled successfully" : "MFA disabled successfully")
                .build();
    }

    @Transactional
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
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

        userRepository.save(user);
        return ProfileResponse.fromUser(user);
    }
}