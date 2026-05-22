package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CatalogAuthService {
    private final UserRepository userRepository;

    public CatalogAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getSellerName(String sellerId) {
        return userRepository.findByEmail(sellerId)
                .map(user -> user.getDisplayName())
                .orElse("Unknown Seller");
    }
}
