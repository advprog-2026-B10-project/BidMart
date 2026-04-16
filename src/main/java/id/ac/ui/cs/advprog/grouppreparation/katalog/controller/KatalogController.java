package id.ac.ui.cs.advprog.grouppreparation.katalog.controller;

import id.ac.ui.cs.advprog.grouppreparation.katalog.model.Katalog;
import id.ac.ui.cs.advprog.grouppreparation.katalog.service.KatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/katalog")
@CrossOrigin(origins = "*")
public class KatalogController {

    private final KatalogService katalogService;

    @Autowired
    public KatalogController(KatalogService katalogService) {
        this.katalogService = katalogService;
    }

    @PostMapping
    public ResponseEntity<Katalog> createListing(@RequestBody Katalog katalog) {
        Katalog newKatalog = katalogService.createListing(katalog);
        return ResponseEntity.ok(newKatalog);
    }

    @GetMapping
    public ResponseEntity<List<Katalog>> getAllKatalog() {
        return ResponseEntity.ok(katalogService.getAllKatalog());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Katalog> getKatalogById(@PathVariable Long id) {
        Katalog katalog = katalogService.getKatalogById(id);
        if (katalog != null) {
            return ResponseEntity.ok(katalog);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Katalog>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cat,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        return ResponseEntity.ok(katalogService.searchKatalog(q, cat, min, max));
    }

    // Endpoint: PUT /api/katalog/update/5
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String deskripsi = body.get("deskripsi");
            String gambar = body.get("gambar");
            Katalog updated = katalogService.updateListing(id, deskripsi, gambar);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            katalogService.cancelListing(id);
            return ResponseEntity.ok(Map.of("message", "Listing berhasil dibatalkan"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}