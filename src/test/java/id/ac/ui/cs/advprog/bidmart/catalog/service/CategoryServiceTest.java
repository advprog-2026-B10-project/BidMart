package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Category;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Elektronik", null);
        category.setId(1L);
    }

    @Test
    void testCreateKategori() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        Category created = categoryService.createKategori(category);
        assertEquals("Elektronik", created.getNama());
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    void testGetHierarkiKategori() {
        when(categoryRepository.findByParentIsNull()).thenReturn(Arrays.asList(category));
        List<Category> list = categoryService.getHierarkiKategori();
        assertEquals(1, list.size());
        assertEquals("Elektronik", list.get(0).getNama());
        verify(categoryRepository, times(1)).findByParentIsNull();
    }
}
