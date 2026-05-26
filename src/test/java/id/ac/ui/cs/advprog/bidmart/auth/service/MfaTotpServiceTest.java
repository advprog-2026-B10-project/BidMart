package id.ac.ui.cs.advprog.bidmart.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MfaTotpServiceTest {

    private MfaTotpService mfaTotpService;

    @BeforeEach
    void setUp() {
        mfaTotpService = new MfaTotpService();
    }

    @Test
    void generateSecret_Returns32CharBase32() {
        String secret = mfaTotpService.generateSecret();
        assertNotNull(secret);
        assertEquals(32, secret.length());
        assertTrue(secret.matches("^[A-Z2-7]+=*$"));
    }

    @Test
    void generateCurrentCode_Returns6Digits() {
        String secret = mfaTotpService.generateSecret();
        String code = mfaTotpService.generateCurrentCode(secret);
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void verifyCode_ValidCode_ReturnsTrue() {
        String secret = mfaTotpService.generateSecret();
        String code = mfaTotpService.generateCurrentCode(secret);
        assertTrue(mfaTotpService.verifyCode(secret, code));
    }

    @Test
    void verifyCode_InvalidCode_ReturnsFalse() {
        String secret = mfaTotpService.generateSecret();
        assertFalse(mfaTotpService.verifyCode(secret, "000000"));
    }

    @Test
    void buildOtpAuthUri_ReturnsValidUri() {
        String secret = mfaTotpService.generateSecret();
        String uri = mfaTotpService.buildOtpAuthUri("user@test.com", secret);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("issuer=BidMart"));
        assertTrue(uri.contains("algorithm=SHA1"));
    }
}
