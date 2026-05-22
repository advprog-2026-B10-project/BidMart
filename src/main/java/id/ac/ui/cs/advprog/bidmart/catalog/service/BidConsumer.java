package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.repository.CatalogRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class BidConsumer {

    @Autowired
    private CatalogRepository catalogRepository;

    @RabbitListener(queues = "bid_queue")
    public void receiveBidMessage(Map<String, Object> message) {
        Long catalogId = Long.valueOf(message.get("catalogId").toString());
        Double newPrice = Double.valueOf(message.get("newPrice").toString());

        catalogRepository.findById(catalogId).ifPresent(catalog -> {
            // Bandingkan: cuma update kalau bid baru lebih tinggi dari harga sekarang
            Double current = catalog.getHargaSekarang() != null ? catalog.getHargaSekarang() : catalog.getHargaAwal();

            if (newPrice > current) {
                catalog.setHargaSekarang(newPrice);
                catalogRepository.save(catalog);
                System.out.println(">>> HARGA UPDATE: Catalog " + catalogId + " sekarang " + newPrice);
            }
        });
    }
}