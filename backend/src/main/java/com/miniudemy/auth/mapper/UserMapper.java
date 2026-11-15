package com.miniudemy.auth.mapper;

import org.mapstruct.Mapper;

import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserResponse;
import com.miniudemy.common.mapper.MapStructConfig;

/**
 * {@link User} → {@link UserResponse} dönüşümü. Uygulaması derleme sırasında MapStruct tarafından üretilir
 * ({@code UserMapperImpl}); alanlar adlarına göre eşlenir. {@code UserResponse}'a kaynağı olmayan bir alan
 * eklenirse derleme durur ({@link MapStructConfig}).
 */
@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);
}
