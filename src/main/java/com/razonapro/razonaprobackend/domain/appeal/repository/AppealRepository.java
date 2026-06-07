package com.razonapro.razonaprobackend.domain.appeal.repository;

import com.razonapro.razonaprobackend.domain.appeal.model.Appeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, String> {

    boolean existsByStudentIdAndProgramIdAndStatus(String studentId, String programId, String status);

    /** Última apelación del estudiante (para mostrar su estado/respuesta). */
    Optional<Appeal> findTopByStudentIdAndProgramIdOrderByCreatedAtDesc(String studentId, String programId);

    Page<Appeal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Appeal> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);
}
