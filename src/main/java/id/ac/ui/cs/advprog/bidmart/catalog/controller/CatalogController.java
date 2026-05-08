package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/katalog")
@CrossOrigin(origins = "*")
public class CatalogController {

    private final CatalogService catalogService;

    @Autowired
    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    public ResponseEntity<Catalog> createListing(@RequestBody Catalog catalog) {
        Catalog newCatalog = catalogService.createListing(catalog);
        return ResponseEntity.ok(newCatalog);
    }

    @GetMapping
    public ResponseEntity<List<Catalog>> getAllKatalog() {
        return ResponseEntity.ok(catalogService.getAllKatalog());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Catalog> getKatalogById(@PathVariable Long id) {
        Catalog catalog = catalogService.getKatalogById(id);
        if (catalog != null) {
            return ResponseEntity.ok(catalog);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Catalog>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cat,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        return ResponseEntity.ok(catalogService.searchKatalog(q, cat, min, max));
    }

    // Endpoint: PUT /api/katalog/update/5
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String deskripsi = body.get("deskripsi");
            String gambar = body.get("gambar");
            Catalog updated = catalogService.updateListing(id, deskripsi, gambar);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            catalogService.cancelListing(id);
            return ResponseEntity.ok(Map.of("message", "Listing berhasil dibatalkan"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}