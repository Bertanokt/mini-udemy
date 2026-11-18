package com.miniudemy.enrollment.service;

import java.util.Collection;
import java.util.Map;

import com.miniudemy.enrollment.dto.EnrollmentProgressSnapshot;

/**
 * Kayıtlı kurslar listesinin ihtiyaç duyduğu ilerleme bilgisi (#119). Arayüz {@code enrollment}'ta, uygulaması
 * {@code progress}'te: {@code enrollment} {@code progress}'e bağımlı olmaz, yön hep {@code progress → enrollment} kalır
 * ({@code course/service/EnrollmentCounts} ile aynı kalıp; docs/architecture.md › Modüller arası iletişim).
 */
public interface EnrollmentProgress {

    /** Verilen kayıtların ilerleme özeti tek sorguda; ilerleme kaydı olmayan kayıt haritada yer almaz. */
    Map<Long, EnrollmentProgressSnapshot> snapshots(Collection<Long> enrollmentIds);
}
