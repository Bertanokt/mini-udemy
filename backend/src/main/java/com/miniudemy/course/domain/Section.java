package com.miniudemy.course.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Kursun bölümü; dersleri `position` sırasıyla tutar.
 *
 * Tablo: `sections` (migration `V202611131449__create_course_structure.sql`, ER diyagramı › course). Kurs
 * silinince bölümleri de silinir (ON DELETE CASCADE).
 */
@Entity
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    /** Kurs içindeki sıra (0'dan başlar). */
    @Column(nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Dersler `position` sırasıyla yüklenir; bölümle birlikte kaydedilir ve silinir. */
    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Lesson> lessons = new ArrayList<>();

    /** JPA için. */
    protected Section() {
    }

    public Section(String title, int position) {
        this.title = title;
        this.position = position;
    }

    /** Yalnız {@link Course#addSection(Section)} çağırır. */
    void assignTo(Course course) {
        this.course = course;
    }

    /** Dersi bu bölüme bağlar; iki yönlü ilişkinin iki ucu da güncellenir. */
    public void addLesson(Lesson lesson) {
        lesson.assignTo(this);
        lessons.add(lesson);
    }

    /** Bölüm başlığını değiştirir (#24, sözleşme › updateSection). */
    public void rename(String title) {
        this.title = title;
    }

    /** Sırayı değiştirir; yalnız {@link Course} yeniden sıralarken çağırır. */
    void moveTo(int position) {
        this.position = position;
    }

    /** Dersi bölümden çıkarır (orphanRemoval siler) ve kalan derslerin sırasını boşluksuz yeniden numaralar. */
    public void removeLesson(Lesson lesson) {
        lessons.remove(lesson);
        renumberLessons();
    }

    /**
     * Dersi bölüm içinde verilen sıraya taşır, aradakiler kayar. Sıra 0..ders sayısı-1 aralığında olmalıdır;
     * aralık dışı {@link IllegalArgumentException} (servis bunu 400'e çevirmeden önce kendisi denetler).
     */
    public void moveLesson(Lesson lesson, int newPosition) {
        if (newPosition < 0 || newPosition >= lessons.size()) {
            throw new IllegalArgumentException("Sıra 0 ile " + (lessons.size() - 1) + " arasında olmalıdır.");
        }
        lessons.remove(lesson);
        lessons.add(newPosition, lesson);
        // KIRMIZI-B6 renumberLessons();
    }

    private void renumberLessons() {
        for (int i = 0; i < lessons.size(); i++) {
            lessons.get(i).moveTo(i);
        }
    }

    /** Bölümdeki derslerin toplam süresi (saniye). */
    public int getTotalDurationSeconds() {
        return lessons.stream().mapToInt(Lesson::getDurationSeconds).sum();
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public int getPosition() {
        return position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Lesson> getLessons() {
        return Collections.unmodifiableList(lessons);
    }
}
