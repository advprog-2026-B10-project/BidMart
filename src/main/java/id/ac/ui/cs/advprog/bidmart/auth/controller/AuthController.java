package id.ac.ui.cs.advprog.bidmart.auth.controller;

import id.ac.ui.cs.advprog.bidmart.auth.dto.*;

import id.ac.ui.cs.advprog.bidmart.auth.service.AuthService;
import id.ac.ui.cs.advprog.bidmart.auth.service.AdminPermissionService;
import id.ac.ui.cs.advprog.bidmart.auth.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AdminPermissionService adminPermissionService;

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
    @RequiresPermission("user:manage")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAdminUsers());
    }

    @PostMapping("/admin/users/{id}/sessions/revoke")
    @RequiresPermission("user:manage")
    public ResponseEntity<AdminSessionRevokeResponse> revokeUserSessions(@PathVariable Long id) {
        return ResponseEntity.ok(authService.revokeUserSessions(id));
    }

    @PatchMapping("/admin/users/{id}/role")
    @RequiresPermission("user:manage")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(authService.updateUserRole(id, request.getRole(), authentication.getName()));
    }

    @PostMapping("/admin/users/{id}/disable")
    @RequiresPermission("user:disable")
    public ResponseEntity<String> disableUser(@PathVariable Long id) {
        authService.disableUser(id);
        return ResponseEntity.ok("User disabled successfully and all sessions revoked.");
    }

    @GetMapping("/admin/permissions")
    @RequiresPermission("role:manage")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(adminPermissionService.getAllPermissions());
    }

    @PostMapping("/admin/permissions")
    @RequiresPermission("role:manage")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.ok(adminPermissionService.createPermission(request));
    }

    @DeleteMapping("/admin/permissions/{id}")
    @RequiresPermission("role:manage")
    public ResponseEntity<String> deletePermission(@PathVariable Long id) {
        adminPermissionService.deletePermission(id);
        return ResponseEntity.ok("Permission deleted successfully.");
    }

    @GetMapping("/admin/role-groups")
    @RequiresPermission("role:manage")
    public ResponseEntity<List<RoleGroupResponse>> getAllRoleGroups() {
        return ResponseEntity.ok(adminPermissionService.getAllRoleGroups());
    }

    @PostMapping("/admin/role-groups")
    @RequiresPermission("role:manage")
    public ResponseEntity<RoleGroupResponse> createRoleGroup(@Valid @RequestBody CreateRoleGroupRequest request) {
        return ResponseEntity.ok(adminPermissionService.createRoleGroup(request));
    }

    @PutMapping("/admin/role-groups/{id}")
    @RequiresPermission("role:manage")
    public ResponseEntity<RoleGroupResponse> updateRoleGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleGroupRequest request) {
        return ResponseEntity.ok(adminPermissionService.updateRoleGroup(id, request));
    }

    @DeleteMapping("/admin/role-groups/{id}")
    @RequiresPermission("role:manage")
    public ResponseEntity<String> deleteRoleGroup(@PathVariable Long id) {
        adminPermissionService.deleteRoleGroup(id);
        return ResponseEntity.ok("Role group deleted successfully.");
    }

    @PutMapping("/admin/users/{id}/roles")
    @RequiresPermission("role:manage")
    public ResponseEntity<String> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        adminPermissionService.updateUserRoleGroups(id, request.getRoleGroupIds());
        return ResponseEntity.ok("User roles updated successfully.");
    }

    @GetMapping("/profile")
    @RequiresPermission("profile:view")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        ProfileResponse response = authService.getProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    @RequiresPermission("profile:edit")
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

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok("If the email is registered and not verified, a new verification link has been sent.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("If the email is registered, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully. You can now login with your new password.");
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