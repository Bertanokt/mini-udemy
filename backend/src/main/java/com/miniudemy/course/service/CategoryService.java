package com.miniudemy.course.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Category;
import com.miniudemy.course.dto.CategoryRequest;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.mapper.CategoryMapper;
import com.miniudemy.course.repository.CategoryRepository;
import com.miniudemy.course.repository.CourseRepository;

/** Kategoriler: liste (#109) ve yönetici işlemleri ekle / yeniden adlandır / sil (#118). */
@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final CourseRepository courseRepository;
    private final CategoryMapper mapper;

    public CategoryService(CategoryRepository repository, CourseRepository courseRepository, CategoryMapper mapper) {
        this.repository = repository;
        this.courseRepository = courseRepository;
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

    /** Yeni kategori. Ad büyük/küçük harf duyarsız tekildir; aynı ad varsa 409. */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw duplicateName();
        }
        return mapper.toResponse(save(new Category(request.name())));
    }

    /** Kategoriyi yeniden adlandırır. Aynı ad başka kategoride varsa 409; kendi adının harf değişimi serbesttir. */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = find(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw duplicateName();
        }
        category.rename(request.name());
        return mapper.toResponse(save(category));
    }

    /** Bir kursta (durumu ne olursa olsun) kullanılan kategori silinemez (409). */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = find(id);
        if (courseRepository.existsByCategoryId(id)) {
            throw new ConflictException("Bu kategori bir kursta kullanıldığı için silinemez.");
        }
        repository.delete(category);
    }

    private Category find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Kategori bulunamadı."));
    }

    /** Kontrol ile yazma arasında aynı adı başka bir istek eklediyse veritabanı indeksi 409'a çevrilir. */
    private Category save(Category category) {
        try {
            return repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw duplicateName();
        }
    }

    private static ConflictException duplicateName() {
        String message = "Bu adda bir kategori zaten var.";
        return new ConflictException(message, Map.of("name", message));
    }
}
