package id.ac.ui.cs.advprog.bidmart.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private String email;
    private String role;
    private boolean mfaRequired;
    private String message;
    private String mfaChallengeToken;
}