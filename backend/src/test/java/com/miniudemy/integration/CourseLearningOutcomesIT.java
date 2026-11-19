package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Kurs kazanımları veritabanında hiçbir zaman NULL değildir (#184). Kolon NULL olabildiğinde Hibernate okunan kursun
 * alanına null yazıyor, {@code Course.getLearningOutcomes()} NullPointerException fırlatıyor ve kurs detayı 500
 * dönüyordu. Kural migration'da: kolon {@code NOT NULL DEFAULT '{}'}.
 */
class CourseLearningOutcomesIT extends IntegrationTestBase {

    @Test
    void courseInsertedWithoutLearningOutcomes_detailReturnsEmptyList() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long instructorId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'mehmet@example.com'", Long.class);
        // Seed ya da elle yazılmış SQL gibi: learning_outcomes kolonu hiç verilmez.
        long courseId = jdbc.queryForObject("""
                INSERT INTO courses (instructor_id, category_id, title, description, level)
                VALUES (?, (SELECT id FROM categories WHERE name = 'Yazılım'), 'Sıfırdan Java', 'Açıklama', 'BEGINNER')
                RETURNING id
                """, Long.class, instructorId);

        ResponseEntity<JsonNode> detail = get("/api/courses/" + courseId, instructor);

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().get("learningOutcomes").isArray()).isTrue();
        assertThat(detail.getBody().get("learningOutcomes")).isEmpty();
    }

    @Test
    void writingNullLearningOutcomes_isRejectedByDatabase() {
        registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long instructorId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'mehmet@example.com'", Long.class);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO courses (instructor_id, category_id, title, description, level, learning_outcomes)
                VALUES (?, (SELECT id FROM categories WHERE name = 'Yazılım'), 'Sıfırdan Java', 'Açıklama', 'BEGINNER',
                        NULL)
                """, instructorId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("learning_outcomes");
    }
}
