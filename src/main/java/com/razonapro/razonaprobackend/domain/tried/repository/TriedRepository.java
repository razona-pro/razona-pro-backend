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

    /** Historial admin: todos los intentos, con filtros opcionales por estudiante y estado. */
    @Query("""
        SELECT t FROM Tried t
        WHERE (:studentId IS NULL OR t.studentId = :studentId)
          AND (:status    IS NULL OR t.status    = :status)
    """)
    Page<Tried> findForAdmin(@Param("studentId") String studentId,
                             @Param("status") String status,
                             Pageable pageable);

    long countByStatus(String status);

    /** Suma de score y conteo para ranking: solo EXAM/TIMED FINISHED, en el período [start,end] (null = sin límite). */
    @Query("""
        SELECT COALESCE(SUM(t.score), 0), COUNT(t)
        FROM Tried t, com.razonapro.razonaprobackend.domain.test.model.Test te
        WHERE te.testId = t.testId AND te.competenceId = t.competenceId
          AND t.studentId = :studentId AND t.programId = :programId
          AND t.status = 'FINISHED' AND t.score IS NOT NULL
          AND te.testMode IN ('EXAM','TIMED')
          AND (:start IS NULL OR COALESCE(t.finishedAt, t.attemptTimestamp) >= :start)
          AND (:end   IS NULL OR COALESCE(t.finishedAt, t.attemptTimestamp) <= :end)
    """)
    java.util.List<Object[]> sumTriedsForRanking(@Param("studentId") String studentId,
                                                 @Param("programId") String programId,
                                                 @Param("start") java.time.LocalDateTime start,
                                                 @Param("end")   java.time.LocalDateTime end);

    /**
     * % de intentos "satisfactorios" = con al menos 60% de aciertos (correctas/total).
     * Se basa en aciertos, NO en el score crudo (el score ahora son puntos ponderados, no /100).
     */
    @Query("""
        SELECT COALESCE(
          (SUM(CASE WHEN t.totalQuestions > 0
                      AND (COALESCE(t.correctAnswers, 0) * 100.0 / t.totalQuestions) >= 60
                    THEN 1.0 ELSE 0.0 END) / COUNT(t)) * 100,
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