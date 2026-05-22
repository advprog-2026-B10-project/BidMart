package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Category;
import id.ac.ui.cs.advprog.bidmart.catalog.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kategori")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<Category> createKategori(@RequestBody Category category) {
        Category newCategory = categoryService.createKategori(category);
        return ResponseEntity.ok(newCategory);
    }

    @GetMapping("/hierarki")
    public ResponseEntity<List<Category>> getHierarkiKategori() {
        return ResponseEntity.ok(categoryService.getHierarkiKategori());
    }
}
