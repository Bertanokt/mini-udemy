package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Category;
import com.miniudemy.course.dto.CategoryRequest;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.mapper.CategoryMapper;
import com.miniudemy.course.repository.CategoryRepository;
import com.miniudemy.course.repository.CourseRepository;

/** Kategori listesi (#109; sıra Türkçe alfabeye göre) ve yönetici işlemleri (#118). */
class CategoryServiceTest {

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CategoryService service =
            new CategoryService(repository, courseRepository, Mappers.getMapper(CategoryMapper.class));

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

    @Test
    void createCategory_withNewName_savesAndReturnsIt() {
        given(repository.existsByNameIgnoreCase("Müzik")).willReturn(false);
        given(repository.saveAndFlush(any(Category.class))).willAnswer(call -> {
            Category saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 9L);
            return saved;
        });

        assertThat(service.createCategory(new CategoryRequest("  Müzik "))).isEqualTo(new CategoryResponse(9L, "Müzik"));
    }

    @Test
    void createCategory_withSameNameInOtherCase_throwsConflictWithNameError() {
        given(repository.existsByNameIgnoreCase("yazılım")).willReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CategoryRequest("yazılım")))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrors()).containsKey("name"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createCategory_whenIndexRejectsRacingDuplicate_throwsConflict() {
        given(repository.saveAndFlush(any(Category.class))).willThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.createCategory(new CategoryRequest("Müzik")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCategory_withFreeName_renamesIt() {
        Category dil = category(4, "Dil");
        given(repository.findById(4L)).willReturn(Optional.of(dil));
        given(repository.saveAndFlush(dil)).willReturn(dil);

        assertThat(service.updateCategory(4L, new CategoryRequest("Yabancı Dil")))
                .isEqualTo(new CategoryResponse(4L, "Yabancı Dil"));
    }

    @Test
    void updateCategory_withNameOfAnotherCategory_throwsConflict() {
        given(repository.findById(4L)).willReturn(Optional.of(category(4, "Dil")));
        given(repository.existsByNameIgnoreCaseAndIdNot("tasarım", 4L)).willReturn(true);

        assertThatThrownBy(() -> service.updateCategory(4L, new CategoryRequest("tasarım")))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateCategory_whenMissing_throwsNotFound() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(99L, new CategoryRequest("Müzik")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteCategory_whenUnused_deletesIt() {
        Category dil = category(4, "Dil");
        given(repository.findById(4L)).willReturn(Optional.of(dil));
        given(courseRepository.existsByCategoryId(4L)).willReturn(false);

        service.deleteCategory(4L);

        verify(repository).delete(dil);
    }

    @Test
    void deleteCategory_whenUsedByACourse_throwsConflictAndKeepsIt() {
        given(repository.findById(1L)).willReturn(Optional.of(category(1, "Yazılım")));
        given(courseRepository.existsByCategoryId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.deleteCategory(1L)).isInstanceOf(ConflictException.class);
        verify(repository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_whenMissing_throwsNotFound() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCategory(99L)).isInstanceOf(NotFoundException.class);
    }

    private static Category category(long id, String name) {
        Category category = new Category(name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
