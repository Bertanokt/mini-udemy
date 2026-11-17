package com.miniudemy.auth.mapper;

import org.mapstruct.Mapper;

import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.AdminUserResponse;
import com.miniudemy.common.mapper.MapStructConfig;

/** {@link User} → {@link AdminUserResponse}; parola özeti DTO'da olmadığı için hiçbir yere taşınmaz. */
@Mapper(config = MapStructConfig.class)
public interface AdminUserMapper {

    AdminUserResponse toResponse(User user);
}
