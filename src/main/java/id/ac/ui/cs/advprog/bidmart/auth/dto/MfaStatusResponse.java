package id.ac.ui.cs.advprog.bidmart.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaStatusResponse {
    private boolean mfaEnabled;
    private String message;
    private String secret;
    private String otpauthUri;
}
