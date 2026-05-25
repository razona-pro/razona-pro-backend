package com.razonapro.razonaprobackend.domain.tried.repository;

import com.razonapro.razonaprobackend.domain.tried.model.Tried;
import com.razonapro.razonaprobackend.shared.ids.TriedId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriedRepository extends JpaRepository<Tried, TriedId> {

    Page<Tried> findByStudentIdAndProgramId(String studentId, String programId, Pageable pageable);

    Optional<Tried> findByTriedId(String triedId);

    @Query("SELECT t FROM Tried t WHERE t.studentId = :studentId AND t.programId = :programId AND t.status = 'IN_PROGRESS'")
    List<Tried> findInProgressByStudent(String studentId, String programId);

    long countByStatus(String status);

    @Query("""
        SELECT COALESCE(
          (SUM(CASE WHEN t.score >= 60 THEN 1.0 ELSE 0.0 END) / COUNT(t)) * 100,
          0
        )
        FROM Tried t WHERE t.status = 'FINISHED'
    """)
    double satisfactionPercentage();
}