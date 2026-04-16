package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Category;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createKategori(Category category) {
        return categoryRepository.save(category);
    }

    public List<Category> getHierarkiKategori() {
        return categoryRepository.findByParentIsNull();
    }
}
