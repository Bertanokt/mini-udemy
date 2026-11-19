-- Kurs kazanımları hiçbir zaman NULL değildir; kazanımı olmayan kurs boş dizi '{}' tutar (#184).
-- Kolon NULL olabildiğinde Hibernate okunan kursun alanına null yazıyor ve Course.getLearningOutcomes()
-- NullPointerException fırlatıyordu (kurs detayı ve katalog 500). Önce mevcut NULL'lar boş diziye çevrilir.
UPDATE courses SET learning_outcomes = '{}' WHERE learning_outcomes IS NULL;

ALTER TABLE courses
    ALTER COLUMN learning_outcomes SET DEFAULT '{}',
    ALTER COLUMN learning_outcomes SET NOT NULL;
