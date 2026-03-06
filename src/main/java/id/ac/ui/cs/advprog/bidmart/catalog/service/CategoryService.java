package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Category;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository kategoriRepository;

    @Autowired
    public CategoryService(CategoryRepository kategoriRepository) {
        this.kategoriRepository = kategoriRepository;
    }

    public Category createKategori(Category kategori) {
        return kategoriRepository.save(kategori);
    }

    public List<Category> getHierarkiKategori() {
        return kategoriRepository.findByParentIsNull();
    }
}
