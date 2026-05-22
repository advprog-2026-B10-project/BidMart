package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CatalogAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CatalogAuthService catalogAuthService;

    @Test
    void testGetSellerNameSuccess() {
        User seller = User.builder()
                .email("seller@example.com")
                .displayName("Jane Doe")
                .role(Role.SELLER)
                .isEnabled(true)
                .build();

        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));

        String sellerName = catalogAuthService.getSellerName("seller@example.com");
        assertEquals("Jane Doe", sellerName);
    }

    @Test
    void testGetSellerNameNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        String sellerName = catalogAuthService.getSellerName("unknown@example.com");
        assertEquals("Unknown Seller", sellerName);
    }
}
