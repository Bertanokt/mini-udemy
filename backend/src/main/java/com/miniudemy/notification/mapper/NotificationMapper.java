package com.miniudemy.notification.mapper;

import org.mapstruct.Mapper;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.notification.domain.Notification;
import com.miniudemy.notification.dto.NotificationResponse;

/** {@code Notification} -> {@code NotificationResponse}. {@code read}, entity'nin {@code isRead()}'inden gelir. */
@Mapper(config = MapStructConfig.class)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
