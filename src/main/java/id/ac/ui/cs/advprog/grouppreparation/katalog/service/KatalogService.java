package id.ac.ui.cs.advprog.grouppreparation.katalog.service;

import id.ac.ui.cs.advprog.grouppreparation.katalog.model.Katalog;
import id.ac.ui.cs.advprog.grouppreparation.katalog.repository.KatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

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

    public Katalog getKatalogById(Long id) {
        return katalogRepository.findById(id).orElse(null);
    }

    public List<Katalog> searchKatalog(String keyword, Long categoryId, Double minPrice, Double maxPrice) {
        return katalogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Cari di kolom 'judul' ATAU 'deskripsi'
            if (keyword != null && !keyword.isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate searchJudul = cb.like(cb.lower(root.get("judul")), pattern);
                Predicate searchDeskripsi = cb.like(cb.lower(root.get("deskripsi")), pattern);

                predicates.add(cb.or(searchJudul, searchDeskripsi));
            }
            // Filter Kategori
            if (categoryId != null) {
                // Lu ambil field 'kategori' (objek), terus ambil field 'id' di dalemnya
                predicates.add(cb.equal(root.get("kategori").get("id"), categoryId));
            }
            // Filter Rentang Harga
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("hargaAwal"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("hargaAwal"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    // FITUR 2 & 3: Validasi "No Bids" sebelum Update/Cancel
    public Katalog updateListing(Long id, String deskripsi, String gambar) {
        Katalog item = katalogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item tidak ditemukan"));

        if (katalogRepository.hasBids(id)) {
            throw new IllegalStateException("Gagal: Sudah ada penawaran masuk!");
        }

        item.setDeskripsi(deskripsi);
        item.setGambar(gambar);
        return katalogRepository.save(item);
    }

    public void cancelListing(Long id) {
        if (katalogRepository.hasBids(id)) {
            throw new IllegalStateException("Gagal: Tidak bisa cancel karena sudah ada penawaran!");
        }
        katalogRepository.deleteById(id);
    }
}
