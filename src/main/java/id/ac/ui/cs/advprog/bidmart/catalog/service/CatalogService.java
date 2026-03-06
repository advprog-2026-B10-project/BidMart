package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CatalogRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogRepository katalogRepository;

    @Autowired
    public CatalogService(CatalogRepository katalogRepository) {
        this.katalogRepository = katalogRepository;
    }

    public Catalog createListing(Catalog katalog) {
        return katalogRepository.save(katalog);
    }

    // Method buat dipanggil Controller
    public List<Catalog> getAllKatalog() {
        return katalogRepository.findAll();
    }

    public Catalog getKatalogById(Long id) {
        return katalogRepository.findById(id).orElse(null);
    }
}
