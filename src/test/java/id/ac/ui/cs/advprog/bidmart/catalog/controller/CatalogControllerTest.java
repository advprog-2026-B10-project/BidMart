package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.entity.Category;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import id.ac.ui.cs.advprog.bidmart.catalog.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private CatalogController catalogController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Catalog catalog;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogController).build();
        Category category = new Category("Electronics", null);
        category.setId(1L);
        catalog = new Catalog("Iphone 13", "Mulus", "img.png", category, 5000000.0, 7000000.0, 120, "seller123");
        catalog.setId(1L);
        catalog.setSellerName("John Doe");
    }

    @Test
    void testCreateListing() throws Exception {
        when(catalogService.createListing(any(Catalog.class))).thenReturn(catalog);

        mockMvc.perform(post("/api/katalog")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catalog)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.judul").value("Iphone 13"));
    }

    @Test
    void testGetAllKatalog() throws Exception {
        when(catalogService.getAllKatalog()).thenReturn(Arrays.asList(catalog));

        mockMvc.perform(get("/api/katalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].judul").value("Iphone 13"));
    }

    @Test
    void testGetKatalogByIdFound() throws Exception {
        when(catalogService.getKatalogById(1L)).thenReturn(catalog);

        mockMvc.perform(get("/api/katalog/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerName").value("John Doe"));
    }

    @Test
    void testGetKatalogByIdNotFound() throws Exception {
        when(catalogService.getKatalogById(2L)).thenReturn(null);

        mockMvc.perform(get("/api/katalog/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSearchKatalog() throws Exception {
        when(catalogService.searchKatalog("Iphone", 1L, 1000.0, 10000000.0, null))
                .thenReturn(Arrays.asList(catalog));

        mockMvc.perform(get("/api/katalog/search")
                .param("q", "Iphone")
                .param("cat", "1")
                .param("min", "1000.0")
                .param("max", "10000000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].judul").value("Iphone 13"));
    }

    @Test
    void testUpdateListingSuccess() throws Exception {
        when(catalogService.updateListing(eq(1L), any(), any()))
                .thenReturn(catalog);

        Map<String, String> body = Map.of("deskripsi", "Updated", "gambar", "img2.png");

        mockMvc.perform(put("/api/katalog/update/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.judul").value("Iphone 13"));
    }

    @Test
    void testUpdateListingFailure() throws Exception {
        when(catalogService.updateListing(eq(1L), any(), any()))
                .thenThrow(new IllegalStateException("Gagal: Sudah ada penawaran masuk!"));

        Map<String, String> body = Map.of("deskripsi", "Updated");

        mockMvc.perform(put("/api/katalog/update/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Gagal: Sudah ada penawaran masuk!"));
    }

    @Test
    void testCancelListingSuccess() throws Exception {
        doNothing().when(catalogService).cancelListing(1L);

        mockMvc.perform(delete("/api/katalog/cancel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Listing berhasil dibatalkan"));
    }

    @Test
    void testCancelListingFailure() throws Exception {
        doThrow(new IllegalStateException("Gagal: Tidak bisa cancel")).when(catalogService).cancelListing(1L);

        mockMvc.perform(delete("/api/katalog/cancel/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Gagal: Tidak bisa cancel"));
    }
    
    @Test
    void testValidateKatalogExists() throws Exception {
        when(catalogService.getKatalogById(1L)).thenReturn(catalog);

        mockMvc.perform(get("/api/katalog/1/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testValidateKatalogNotExists() throws Exception {
        when(catalogService.getKatalogById(2L)).thenReturn(null);

        mockMvc.perform(get("/api/katalog/2/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
