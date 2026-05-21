package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CatalogServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private CatalogAuthService catalogAuthService;

    @InjectMocks
    private CatalogService catalogService;

    private Catalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new Catalog("Test", "Desc", "img.jpg", null, 100.0, 200.0, 60, "seller1");
        catalog.setId(1L);
    }

    @Test
    void testCreateListing() {
        when(catalogRepository.save(any(Catalog.class))).thenReturn(catalog);
        Catalog created = catalogService.createListing(catalog);
        assertEquals("Test", created.getJudul());
        verify(catalogRepository, times(1)).save(catalog);
    }

    @Test
    void testGetAllKatalog() {
        when(catalogRepository.findAll()).thenReturn(Arrays.asList(catalog));
        List<Catalog> list = catalogService.getAllKatalog();
        assertEquals(1, list.size());
    }

    @Test
    void testGetKatalogByIdFoundWithSellerName() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        when(catalogAuthService.getSellerName("seller1")).thenReturn("John Seller");

        Catalog found = catalogService.getKatalogById(1L);
        assertNotNull(found);
        assertEquals("John Seller", found.getSellerName());
    }

    @Test
    void testGetKatalogByIdNotFound() {
        when(catalogRepository.findById(2L)).thenReturn(Optional.empty());
        Catalog found = catalogService.getKatalogById(2L);
        assertNull(found);
    }



    @Test
    void testUpdateListingSuccess() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        when(catalogRepository.hasBids(1L)).thenReturn(false);
        when(catalogRepository.save(any(Catalog.class))).thenReturn(catalog);

        Catalog updated = catalogService.updateListing(1L, "New Desc", "new.jpg");
        assertEquals("New Desc", updated.getDeskripsi());
        assertEquals("new.jpg", updated.getGambar());
    }

    @Test
    void testUpdateListingNotFound() {
        when(catalogRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> catalogService.updateListing(2L, "Desc", "img.jpg"));
    }

    @Test
    void testUpdateListingHasBids() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        when(catalogRepository.hasBids(1L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> catalogService.updateListing(1L, "Desc", "img.jpg"));
    }

    @Test
    void testCancelListingSuccess() {
        when(catalogRepository.hasBids(1L)).thenReturn(false);
        catalogService.cancelListing(1L);
        verify(catalogRepository, times(1)).deleteById(1L);
    }

    @Test
    void testCancelListingHasBids() {
        when(catalogRepository.hasBids(1L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> catalogService.cancelListing(1L));
        verify(catalogRepository, never()).deleteById(1L);
    }
}
