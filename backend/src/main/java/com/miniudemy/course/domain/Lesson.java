package com.miniudemy.course.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Bölümdeki ders.
 *
 * Tablo: `lessons` (migration `V202611131449__create_course_structure.sql`, ER diyagramı › course). İçerik
 * dosyası content modülüne ait olduğu için yalnız id olarak tutulur.
 */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    /** content modülündeki dosyanın id'si; dosya tablosu gelince yabancı anahtar eklenecek. */
    @Column(name = "content_file_id")
    private Long contentFileId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private LessonContentType contentType;

    /** Kurs tamamlama ve sertifika için zorunlu mu. */
    @Column(nullable = false)
    private boolean mandatory = true;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    /** Bölüm içindeki sıra (0'dan başlar). */
    @Column(nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA için. */
    protected Lesson() {
    }

    public Lesson(String title, LessonContentType contentType, boolean mandatory, int durationSeconds,
            int position) {
        this.title = title;
        this.contentType = contentType;
        this.mandatory = mandatory;
        this.durationSeconds = durationSeconds;
        this.position = position;
    }

    /** Yalnız {@link Section#addLesson(Lesson)} çağırır. */
    void assignTo(Section section) {
        this.section = section;
    }

    /** Yalnız {@link Section} sıra değiştirirken ve boşluk kapatırken çağırır. */
    void moveTo(int position) {
        this.position = position;
    }

    /**
     * Ders alanlarını günceller. İçerik türü değişirse yüklü dosya bu dersle bağı kopar (eski türün dosyası yeni
     * türe uymaz); dosyanın diskten silinmesi content modülünün işidir.
     */
    public void update(String title, LessonContentType contentType, boolean mandatory, int durationSeconds) {
        if (this.contentType != contentType) {
            this.contentFileId = null;
        }
        this.title = title;
        this.contentType = contentType;
        this.mandatory = mandatory;
        this.durationSeconds = durationSeconds;
    }

    public Long getId() {
        return id;
    }

    public Section getSection() {
        return section;
    }

    public Long getContentFileId() {
        return contentFileId;
    }

    public String getTitle() {
        return title;
    }

    public LessonContentType getContentType() {
        return contentType;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getPosition() {
        return position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
