package com.miniudemy.review.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.review.repository.ReviewRepository;

/**
 * Kurs başına ortalama puan ve yorum sayısı. Katalog kartındaki puan ve {@code minRating} filtresi (#143) buradan
 * beslenecek; katalog kodu bu işte değiştirilmedi. Gizli yorumlar sayıma katılmaz.
 */
@Service
@Transactional(readOnly = true)
public class ReviewStatsService {

    private final ReviewRepository repository;

    public ReviewStatsService(ReviewRepository repository) {
        this.repository = repository;
    }

    /** Verilen her kurs id'si için bir kayıt döner (yorumsuz kurs: null, 0); kurs sayısından bağımsız tek sorgu. */
    public Map<Long, CourseRating> ratingsByCourses(Collection<Long> courseIds) {
        Map<Long, CourseRating> ratings = new HashMap<>();
        courseIds.forEach(id -> ratings.put(id, new CourseRating(null, 0)));
        if (!courseIds.isEmpty()) {
            repository.aggregateVisibleByCourses(courseIds).forEach(row -> ratings.put(row.getCourseId(),
                    new CourseRating(RatingMath.round1(row.getAverage()), row.getTotal())));
        }
        return ratings;
    }
}
