package id.ac.ui.cs.advprog.grouppreparation.katalog.controller;

import id.ac.ui.cs.advprog.grouppreparation.katalog.model.Katalog;
import id.ac.ui.cs.advprog.grouppreparation.katalog.service.KatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}