package com.miniudemy.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.admin.domain.Complaint;
import com.miniudemy.admin.domain.ComplaintStatus;

/** `Complaint` kalıcılığı; sıra {@code pageable} ile gelir. */
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
}
