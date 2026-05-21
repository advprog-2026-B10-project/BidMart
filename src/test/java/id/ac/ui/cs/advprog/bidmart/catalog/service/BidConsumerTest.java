package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BidConsumerTest {

    @Mock
    private CatalogRepository catalogRepository;

    @InjectMocks
    private BidConsumer bidConsumer;

    private Catalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new Catalog();
        catalog.setId(1L);
        catalog.setHargaAwal(100.0);
    }

    @Test
    void testReceiveBidMessageNewPriceHigherThanAwal() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));

        Map<String, Object> message = Map.of("catalogId", 1L, "newPrice", 150.0);
        bidConsumer.receiveBidMessage(message);

        assertEquals(150.0, catalog.getHargaSekarang());
        verify(catalogRepository, times(1)).save(catalog);
    }

    @Test
    void testReceiveBidMessageNewPriceHigherThanSekarang() {
        catalog.setHargaSekarang(120.0);
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));

        Map<String, Object> message = Map.of("catalogId", 1L, "newPrice", 150.0);
        bidConsumer.receiveBidMessage(message);

        assertEquals(150.0, catalog.getHargaSekarang());
        verify(catalogRepository, times(1)).save(catalog);
    }

    @Test
    void testReceiveBidMessageNewPriceLower() {
        catalog.setHargaSekarang(160.0);
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));

        Map<String, Object> message = Map.of("catalogId", 1L, "newPrice", 150.0);
        bidConsumer.receiveBidMessage(message);

        assertEquals(160.0, catalog.getHargaSekarang()); // Should not update
        verify(catalogRepository, never()).save(any());
    }

    @Test
    void testReceiveBidMessageCatalogNotFound() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> message = Map.of("catalogId", 1L, "newPrice", 150.0);
        bidConsumer.receiveBidMessage(message);

        verify(catalogRepository, never()).save(any());
    }
}
