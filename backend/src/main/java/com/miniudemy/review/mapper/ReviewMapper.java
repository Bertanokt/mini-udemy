package com.miniudemy.review.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.review.domain.Review;
import com.miniudemy.review.dto.ReviewResponse;

/** {@code Review} -> {@code ReviewResponse}. Yazar başka modülden geldiği için hazır verilir. */
@Mapper(config = MapStructConfig.class)
public interface ReviewMapper {

    /** Moderasyon gerekçesi bu uçlarda dönmez; yönetici yanıtı (#142) kendi eşlemesini ekler. */
    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "moderationReason", ignore = true)
    ReviewResponse toResponse(Review review, UserSummary author);

    /** Yönetici yanıtı (#142): gizli yorumlar ve moderasyon gerekçesi dahil. */
    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "author", source = "author")
    ReviewResponse toAdminResponse(Review review, UserSummary author);
}
