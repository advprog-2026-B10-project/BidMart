package id.ac.ui.cs.advprog.bidmart.catalog.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CatalogAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CatalogAuthService catalogAuthService;

    @Test
    void testGetSellerNameSuccess() {
        ReflectionTestUtils.setField(catalogAuthService, "restTemplate", restTemplate);
        Map<String, Object> mockResponse = Map.of("username", "Jane Doe");
        
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);

        String sellerName = catalogAuthService.getSellerName("seller456");
        assertEquals("Jane Doe", sellerName);
    }

    @Test
    void testGetSellerNameException() {
        ReflectionTestUtils.setField(catalogAuthService, "restTemplate", restTemplate);
        
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("Connection refused"));

        String sellerName = catalogAuthService.getSellerName("seller456");
        assertEquals("Seller Service Unavailable", sellerName);
    }
}
