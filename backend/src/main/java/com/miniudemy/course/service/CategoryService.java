package com.miniudemy.course.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.mapper.CategoryMapper;
import com.miniudemy.course.repository.CategoryRepository;

/** Kategoriler (#109). Yönetimi (ekle/düzenle/sil) #118'de. */
@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryService(CategoryRepository repository, CategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Tüm kategoriler, ada göre Türkçe alfabetik sırayla (sözleşme). Sıralama veritabanında değil burada yapılır:
     * {@code ORDER BY name} PostgreSQL'in kurulu dil ayarına bağlı ve "İş" gibi adlar bazı ayarlarda Z'den sonraya düşer.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        Collator turkish = Collator.getInstance(Locale.forLanguageTag("tr"));
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .sorted(Comparator.comparing(CategoryResponse::name, turkish))
                .toList();
    }
}
