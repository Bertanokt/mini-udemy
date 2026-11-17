package com.miniudemy.course.mapper;

import org.mapstruct.Mapper;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.course.domain.Category;
import com.miniudemy.course.dto.CategoryResponse;

/** {@link Category} → {@link CategoryResponse}. Uygulamasını MapStruct derlemede üretir (ADR-006). */
@Mapper(config = MapStructConfig.class)
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}
