package com.miniudemy.course.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.miniudemy.common.exception.BadRequestException;

/**
 * Eğitmenin oluşturduğu kurs; bölümlerin (ve onların derslerinin) kök nesnesi.
 *
 * Tablo: `courses` (migration `V202611131449__create_course_structure.sql`, ER diyagramı › course). Şema
 * yalnız Flyway ile değişir; bu sınıf `ddl-auto=validate` ile tabloya karşı doğrulanır.
 *
 * Modüller arası kural: eğitmen (auth modülü) ve kapak dosyası (content modülü) entity olarak değil, yalnız
 * id olarak tutulur; course modülü başka modülün entity'sine bağımlı olmaz.
 *
 * Durum geçişlerine bu sınıf değil, State deseninin durum nesneleri karar verir (#85, ADR-010): her işlem
 * {@code status.state()}'e sorulur, izinsizse 409. Durum alanı yalnız bu işlemlerle değişir. Entity API'ye çıkmaz;
 * dışarıya DTO ile verilir.
 */
@Entity
@Table(name = "courses")
public class Course {

    static final String NOT_READY_FOR_REVIEW =
            "Kursu onaya göndermek için en az bir bölüm, bir ders ve içeriği yüklenmiş en az bir ders gerekli.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** `users.id`; auth modülünün entity'sine bağlanmamak için yalnız id. */
    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** content modülündeki kapak dosyasının id'si; dosya tablosu gelince yabancı anahtar eklenecek. */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseLevel level;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "learning_outcomes", columnDefinition = "text[]")
    private List<String> learningOutcomes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    /** Yönetici reddettiğinde gerekçe (İncelemede → Taslak). */
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    /** Yönetici yayından kaldırdığında gerekçe. */
    @Column(name = "unpublish_reason", columnDefinition = "text")
    private String unpublishReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Bölümler `position` sırasıyla yüklenir; kursla birlikte kaydedilir ve silinir. */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Section> sections = new ArrayList<>();

    /** JPA için. */
    protected Course() {
    }

    public Course(Long instructorId, Category category, String title, String description, CourseLevel level,
            List<String> learningOutcomes) {
        this.instructorId = instructorId;
        this.category = category;
        this.title = title;
        this.description = description;
        this.level = level;
        this.learningOutcomes = learningOutcomes == null ? new ArrayList<>() : new ArrayList<>(learningOutcomes);
    }

    /** Bölümü bu kursa bağlar; iki yönlü ilişkinin iki ucu da güncellenir. */
    public void addSection(Section section) {
        section.assignTo(this);
        sections.add(section);
    }

    /** Yeni bölümü sona ekler: sırası mevcut bölüm sayısıdır (#24, sözleşme › createSection). */
    public Section appendSection(String title) {
        Section section = new Section(title, 0); // KIRMIZI-2
        addSection(section);
        return section;
    }

    /** Bölümü kurstan çıkarır (orphanRemoval bölümü ve derslerini siler); kalan bölümler boşluksuz yeniden sıralanır. */
    public void removeSection(Section section) {
        sections.remove(section);
        renumberSections();
    }

    /**
     * Bölümü verilen sıraya taşır, aradakiler kayar. Sıra 0..bölüm sayısı-1 aralığında olmalıdır; aralık dışı
     * {@link IllegalArgumentException} (servis bunu 400'e çevirmeden önce kendisi denetler).
     */
    public void moveSection(Section section, int newPosition) {
        if (newPosition < 0 || newPosition >= sections.size()) {
            throw new IllegalArgumentException("Sıra 0 ile " + (sections.size() - 1) + " arasında olmalıdır.");
        }
        sections.remove(section);
        sections.add(newPosition, section);
        renumberSections();
    }

    private void renumberSections() {
        for (int i = 0; i < sections.size(); i++) {
            sections.get(i).moveTo(i);
        }
    }

    /**
     * Kursun toplam süresi (saniye): tüm bölümlerdeki derslerin `duration_seconds` toplamı. Bölümleri ve
     * dersleri yükler; yalnız toplam gereken listelerde bu yerine veritabanında toplanan bir sorgu tercih
     * edilir.
     */
    public int getTotalDurationSeconds() {
        return sections.stream().mapToInt(Section::getTotalDurationSeconds).sum();
    }

    /** Kurs katalogda görünür (Yayımlandı) mü; durum nesnesine sorulur. */
    public boolean isPublished() {
        return state().isPublished();
    }

    /**
     * Eğitmen kursu ve yapısını (bölüm, ders, sınav) değiştirebilir mi: yalnız Taslak ve Yayından kaldırıldı.
     * Değilse 409 ({@link CourseStateException}).
     */
    public void assertEditable() {
        state().assertEditable();
    }

    /** Kurs silinebilir mi: yalnız Taslak. Değilse 409. */
    public void assertDeletable() {
        state().assertDeletable();
    }

    /**
     * Onaya gönder (Taslak / Yayından kaldırıldı → İncelemede); önceki red gerekçesi temizlenir. Önce durum sorulur
     * (izinsizse 409), sonra içerik: en az bir bölüm, bir ders ve içeriği yüklenmiş en az bir ders yoksa 400 (#27).
     */
    public void submit() {
        CourseStatus target = state().submit();
        if (!hasLessonWithContent()) {
            throw new BadRequestException(NOT_READY_FOR_REVIEW);
        }
        status = target;
        rejectionReason = null;
    }

    /** İçeriği yüklenmiş en az bir ders var mı (bölüm ve ders olmadan olamaz). */
    private boolean hasLessonWithContent() {
        return sections.stream()
                .flatMap(section -> section.getLessons().stream())
                .anyMatch(lesson -> lesson.getContentFileId() != null);
    }

    /** Yönetici onayı (İncelemede → Yayımlandı). */
    public void approve() {
        status = state().approve();
    }

    /**
     * Yönetici reddi (İncelemede → Taslak); gerekçe zorunlu ve kursta saklanır. Gerekçe isteğin DTO'sunda da
     * doğrulanır (#59); buraya boş gelmesi programlama hatasıdır.
     */
    public void reject(String reason) {
        CourseStatus target = state().reject();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Red gerekçesi zorunludur.");
        }
        status = target;
        rejectionReason = reason;
    }

    /** Yayından kaldır (Yayımlandı → Yayından kaldırıldı); gerekçe yönetici için verilir, eğitmen için boş olabilir (FR-38). */
    public void unpublish(String reason) {
        status = state().unpublish();
        unpublishReason = reason;
    }

    private CourseState state() {
        return status.state();
    }

    public Long getId() {
        return id;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public Category getCategory() {
        return category;
    }

    public Long getCoverFileId() {
        return coverFileId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public CourseLevel getLevel() {
        return level;
    }

    public List<String> getLearningOutcomes() {
        return Collections.unmodifiableList(learningOutcomes);
    }

    public CourseStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getUnpublishReason() {
        return unpublishReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Section> getSections() {
        return Collections.unmodifiableList(sections);
    }
}
