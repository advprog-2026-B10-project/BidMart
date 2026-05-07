package id.ac.ui.cs.advprog.bidmart.auth.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MfaTotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String ISSUER = "BidMart";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTES];
        secureRandom.nextBytes(buffer);
        return base32Encode(buffer);
    }

    public String buildOtpAuthUri(String email, String secret) {
        return "otpauth://totp/" + urlEncode(ISSUER + ":" + email)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(ISSUER)
                + "&algorithm=SHA1&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }

        long timeWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            if (generateTotp(secret, timeWindow + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String generateCurrentCode(String secret) {
        long timeWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        return generateTotp(secret, timeWindow);
    }

    private String generateTotp(String secret, long timeWindow) {
        try {
            byte[] keyBytes = base32Decode(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer <<= 8;
            buffer |= b & 0xFF;
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_ALPHABET.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_ALPHABET.charAt(index));
        }

        return result.toString();
    }

    private byte[] base32Decode(String encoded) {
        String normalized = encoded.replace("=", "").toUpperCase();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int current = 0;
        int bitsLeft = 0;

        for (char c : normalized.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }
            current <<= 5;
            current |= value & 0x1F;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((byte) ((current >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }

        return output.toByteArray();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
