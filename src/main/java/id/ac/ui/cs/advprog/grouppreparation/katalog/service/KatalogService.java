package id.ac.ui.cs.advprog.grouppreparation.katalog.service;

import id.ac.ui.cs.advprog.grouppreparation.katalog.model.Katalog;
import id.ac.ui.cs.advprog.grouppreparation.katalog.repository.KatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class KatalogService {

    private final KatalogRepository katalogRepository;

    @Autowired
    public KatalogService(KatalogRepository katalogRepository) {
        this.katalogRepository = katalogRepository;
    }

    public Katalog createListing(Katalog katalog) {
        return katalogRepository.save(katalog);
    }

    // Method buat dipanggil Controller
    public List<Katalog> getAllKatalog() {
        return katalogRepository.findAll();
    }
}
