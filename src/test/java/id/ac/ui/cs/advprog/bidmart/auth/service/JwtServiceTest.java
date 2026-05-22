package id.ac.ui.cs.advprog.bidmart.auth.service;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "MySuperSecretKeyForTestingMySuperSecretKeyForTesting!");
        jwtService.init();

        user = User.builder().email("test@test.com").role(Role.BUYER).build();
    }

    @Test
    void generateAndExtractEmail() {
        String token = jwtService.generateToken(user);
        assertEquals("test@test.com", jwtService.extractEmail(token));
    }

    @Test
    void generateToken_IncludesMfaCompletedClaim() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.extractMfaCompleted(token));
    }

    @Test
    void generateToken_IncludesRole() {
        user.setRole(Role.ADMIN);
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.extractRoles(token).contains("ROLE_ADMIN"));
    }

    @Test
    void generateRefreshToken_IsValidRefreshToken() {
        String token = jwtService.generateRefreshToken(user);
        assertTrue(jwtService.isRefreshTokenValid(token));
    }

    @Test
    void accessToken_IsNotValidRefreshToken() {
        String token = jwtService.generateToken(user);
        assertFalse(jwtService.isRefreshTokenValid(token));
    }

    @Test
    void mfaChallengeToken_ExtractsEmail() {
        String token = jwtService.generateMfaChallengeToken(user);
        assertEquals("test@test.com", jwtService.extractEmail(token));
    }

    @Test
    void mfaChallengeToken_HasTypeClaim() {
        String token = jwtService.generateMfaChallengeToken(user);
        assertTrue(jwtService.isMfaChallengeTokenValid(token));
    }

    @Test
    void accessToken_IsNotValidMfaChallengeToken() {
        String token = jwtService.generateToken(user);
        assertFalse(jwtService.isMfaChallengeTokenValid(token));
    }

    @Test
    void invalidToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token"));
    }

    @Test
    void tokenHasPermissions() {
        RoleGroup rg = RoleGroup.builder().name("BUYER").build();
        user.setRoleGroups(Set.of(rg));
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.extractRoles(token).contains("ROLE_BUYER"));
    }
}
