package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.course.domain.Category;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.mapper.CategoryMapper;
import com.miniudemy.course.repository.CategoryRepository;

/** Kategori listesi (#109): sözleşme "ada göre alfabetik" diyor; sıra Türkçe alfabeye göre. */
class CategoryServiceTest {

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryService service = new CategoryService(repository, Mappers.getMapper(CategoryMapper.class));

    @Test
    void listCategories_returnsAllInTurkishAlphabeticalOrder() {
        // Veritabanından gelen sıra rastgele; "İş" byte sırasında Z'den sonraya düşerdi.
        given(repository.findAll()).willReturn(List.of(
                category(1, "Yazılım"), category(2, "İş"), category(3, "Veri Bilimi"), category(4, "Dil"),
                category(5, "Tasarım"), category(6, "Çizim"), category(7, "Ofis")));

        assertThat(service.listCategories()).extracting(CategoryResponse::name)
                .containsExactly("Çizim", "Dil", "İş", "Ofis", "Tasarım", "Veri Bilimi", "Yazılım");
    }

    @Test
    void listCategories_mapsIdAndName() {
        given(repository.findAll()).willReturn(List.of(category(4, "Dil")));

        assertThat(service.listCategories()).containsExactly(new CategoryResponse(4L, "Dil"));
    }

    private static Category category(long id, String name) {
        Category category = new Category(name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
