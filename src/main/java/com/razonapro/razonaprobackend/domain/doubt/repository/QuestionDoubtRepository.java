// src/main/java/com/razonapro/razonaprobackend/domain/doubt/repository/QuestionDoubtRepository.java
package com.razonapro.razonaprobackend.domain.doubt.repository;

import com.razonapro.razonaprobackend.domain.doubt.model.QuestionDoubt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionDoubtRepository extends JpaRepository<QuestionDoubt, String> {
    Page<QuestionDoubt> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<QuestionDoubt> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    long countByStatus(String status);
}