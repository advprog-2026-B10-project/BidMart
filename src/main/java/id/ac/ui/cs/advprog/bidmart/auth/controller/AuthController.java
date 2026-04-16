package id.ac.ui.cs.advprog.bidmart.auth.controller;

import id.ac.ui.cs.advprog.bidmart.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.bidmart.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmart.auth.dto.ProfileResponse;
import id.ac.ui.cs.advprog.bidmart.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmart.auth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.bidmart.auth.dto.RefreshTokenRequest;
import id.ac.ui.cs.advprog.bidmart.auth.dto.MfaToggleRequest;
import id.ac.ui.cs.advprog.bidmart.auth.dto.MfaStatusResponse;
import id.ac.ui.cs.advprog.bidmart.auth.dto.MfaVerifyRequest;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully. Please verify your email.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        AuthResponse response = authService.verifyMfa(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        authService.verifyUser(token);
        return ResponseEntity.ok("Account verified successfully! You can now login.");
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> users = userRepository.findAll()
                .stream()
                .map(AdminUserResponse::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        ProfileResponse response = authService.getProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        ProfileResponse response = authService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/mfa/toggle")
    public ResponseEntity<MfaStatusResponse> toggleMfa(
            Authentication authentication,
            @Valid @RequestBody MfaToggleRequest request
    ) {
        MfaStatusResponse response = authService.toggleMfa(authentication.getName(), request.getEnabled());
        return ResponseEntity.ok(response);
    }
}