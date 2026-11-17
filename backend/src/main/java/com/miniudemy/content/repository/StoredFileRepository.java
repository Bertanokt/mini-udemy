package com.miniudemy.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.content.domain.StoredFile;

/** Saklanan dosyaların meta verisi. */
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
