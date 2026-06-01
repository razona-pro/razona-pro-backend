package com.razonapro.razonaprobackend.domain.tried.repository;

import com.razonapro.razonaprobackend.domain.tried.model.Tried;
import com.razonapro.razonaprobackend.shared.ids.TriedId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT t.studentId,
               COUNT(t) as totalTrieds,
               COALESCE(AVG(t.score), 0) as avgScore,
               COALESCE(SUM(t.correctAnswers), 0) as totalCorrect,
               COALESCE(SUM(t.totalQuestions), 0) as totalQuestions
        FROM Tried t
        WHERE t.status = 'FINISHED'
        GROUP BY t.studentId
        ORDER BY avgScore DESC
    """)
    List<Object[]> findStudentPerformanceSummary();

    @Query("""
        SELECT t.studentId,
               t.competenceId,
               COUNT(t) as totalTrieds,
               COALESCE(AVG(t.score), 0) as avgScore
        FROM Tried t
        WHERE t.status = 'FINISHED' AND t.studentId = :studentId
        GROUP BY t.studentId, t.competenceId
    """)
    List<Object[]> findStudentPerformanceByCompetence(@Param("studentId") String studentId);
}