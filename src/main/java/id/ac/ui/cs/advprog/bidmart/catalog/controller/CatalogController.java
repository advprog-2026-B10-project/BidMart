package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.catalog.service.CatalogService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/katalog")
@CrossOrigin(origins = "*")
public class CatalogController {

    private final CatalogService katalogService;

    @Autowired
    public CatalogController(CatalogService katalogService) {
        this.katalogService = katalogService;
    }

    @PostMapping
    public ResponseEntity<Catalog> createListing(@RequestBody Catalog katalog) {
        Catalog newKatalog = katalogService.createListing(katalog);
        return ResponseEntity.ok(newKatalog);
    }

    @GetMapping
    public ResponseEntity<List<Catalog>> getAllKatalog() {
        return ResponseEntity.ok(katalogService.getAllKatalog());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Catalog> getKatalogById(@PathVariable Long id) {
        Catalog katalog = katalogService.getKatalogById(id);
        if (katalog != null) {
            return ResponseEntity.ok(katalog);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}