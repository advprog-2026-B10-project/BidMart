package id.ac.ui.cs.advprog.bidmart.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AuthService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String AUTH_URL = "http://auth-service-url/api/auth/profile/";

    public String getSellerName(String sellerId) {
        try {
            // Kita cuma butuh nama, asumsikan Auth Balikin JSON { "username": "Josiah" }
            Map<String, Object> response = restTemplate.getForObject(AUTH_URL + sellerId, Map.class);
            return response != null ? (String) response.get("username") : "Unknown Seller";
        } catch (Exception e) {
            return "Seller Service Unavailable";
        }
    }
}
