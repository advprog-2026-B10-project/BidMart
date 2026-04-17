package id.ac.ui.cs.advprog.bidmart.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import id.ac.ui.cs.advprog.bidmart.auth.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final String SECRET_KEY = "BidMart_Super_Secret_Key_For_AdPro_2026_Semester_4";
    private final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // Access token: 15 minutes (900 seconds)
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15;
    // Refresh token: 7 days
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7;
    // MFA challenge token: 5 minutes
    private static final long MFA_CHALLENGE_EXPIRATION = 1000 * 60 * 5;

    public String generateToken(User user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("roles", roles) 
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("type", "refresh")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public String generateMfaChallengeToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("type", "mfa_challenge")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + MFA_CHALLENGE_EXPIRATION))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(SIGNING_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(SIGNING_KEY) 
                .build()
                .parseSignedClaims(token) 
                .getPayload();           
                
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(SIGNING_KEY)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isMfaChallengeTokenValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(SIGNING_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "mfa_challenge".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessTokenExpiration() {
        return ACCESS_TOKEN_EXPIRATION;
    }
}