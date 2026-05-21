package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;

@Service
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final CatalogAuthService catalogAuthService;

    @Autowired
    public CatalogService(CatalogRepository catalogRepository, CatalogAuthService catalogAuthService) {
        this.catalogRepository = catalogRepository;
        this.catalogAuthService = catalogAuthService;
    }

    public Catalog createListing(Catalog katalog) {
        return catalogRepository.save(katalog);
    }

    // Method buat dipanggil Controller
    public List<Catalog> getAllKatalog() {
        return catalogRepository.findAll();
    }

    public Catalog getKatalogById(Long id) {
        Catalog catalog = catalogRepository.findById(id).orElse(null);
        if (catalog != null && catalog.getSellerId() != null) {
            String sellerName = catalogAuthService.getSellerName(catalog.getSellerId());
            catalog.setSellerName(sellerName);
        }
        return catalog;
    }

    public List<Catalog> searchKatalog(String keyword, Long categoryId, Double minPrice, Double maxPrice, LocalDateTime endTime) {
        return catalogRepository.findAll((root, query, cb) -> {
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
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            // Filter Rentang Harga
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("hargaAwal"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("hargaAwal"), maxPrice));
            }
            
            // Filter Waktu Berakhir (endTime) menggunakan subquery ke entitas Auction
            if (endTime != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Auction> auctionRoot = subquery.from(Auction.class);
                subquery.select(auctionRoot.get("listingId"));
                subquery.where(cb.lessThanOrEqualTo(auctionRoot.get("endTime"), endTime));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    // FITUR 2 & 3: Validasi "No Bids" sebelum Update/Cancel
    public Catalog updateListing(Long id, String deskripsi, String gambar) {
        Catalog item = catalogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item tidak ditemukan"));

        if (catalogRepository.hasBids(id)) {
            throw new IllegalStateException("Gagal: Sudah ada penawaran masuk!");
        }

        item.setDeskripsi(deskripsi);
        item.setGambar(gambar);
        return catalogRepository.save(item);
    }

    public void cancelListing(Long id) {
        if (catalogRepository.hasBids(id)) {
            throw new IllegalStateException("Gagal: Tidak bisa cancel karena sudah ada penawaran!");
        }
        catalogRepository.deleteById(id);
    }
}
