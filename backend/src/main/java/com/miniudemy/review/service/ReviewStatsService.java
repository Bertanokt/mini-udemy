package com.miniudemy.review.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.service.CourseRating;
import com.miniudemy.course.service.CourseRatings;
import com.miniudemy.review.repository.ReviewRepository;

/**
 * Kurs başına ortalama puan ve yorum sayısı. Katalog kartındaki puan ve {@code minRating} filtresi (#143) ile
 * eğitmen istatistikleri (#133) {@code course}'taki {@link CourseRatings} arayüzü üzerinden buradan beslenir.
 * Gizli yorumlar sayıma katılmaz.
 */
@Service
@Transactional(readOnly = true)
public class ReviewStatsService implements CourseRatings {

    private final ReviewRepository repository;

    public ReviewStatsService(ReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public CourseRating ratingOf(Long courseId) {
        return ratingsByCourses(List.of(courseId)).get(courseId);
    }

    @Override
    public Map<String, Long> distributionOf(Long courseId) {
        Map<Integer, Long> counts = new HashMap<>();
        repository.countVisibleByRating(courseId).forEach(row -> counts.put(row.getRating(), row.getTotal()));
        return java.util.Map.of(); // KIRMIZI-Q4
    }

    /** Verilen her kurs id'si için bir kayıt döner (yorumsuz kurs: null, 0); kurs sayısından bağımsız tek sorgu. */
    @Override
    public Map<Long, CourseRating> ratingsByCourses(Collection<Long> courseIds) {
        Map<Long, CourseRating> ratings = new HashMap<>();
        courseIds.forEach(id -> ratings.put(id, new CourseRating(null, 0)));
        if (!courseIds.isEmpty()) {
            repository.aggregateVisibleByCourses(courseIds).forEach(row -> ratings.put(row.getCourseId(),
                    new CourseRating(RatingMath.round1(row.getAverage()), row.getTotal())));
        }
        return ratings;
    }

    /**
     * Yorumu olan kursların ortalaması bellekte aynı yuvarlamayla (bir ondalık) karşılaştırılır: kartta 4.5 görünen
     * kurs {@code minRating=4.5} ile de bulunur. Yorumu olmayan kurs gelmez.
     */
    @Override
    public Set<Long> courseIdsWithAverageAtLeast(double minRating) {
        return repository.aggregateAllVisible().stream()
                .filter(row -> RatingMath.round1(row.getAverage()) >= minRating)
                .map(ReviewRepository.CourseRatingRow::getCourseId)
                .collect(Collectors.toSet());
    }
}
