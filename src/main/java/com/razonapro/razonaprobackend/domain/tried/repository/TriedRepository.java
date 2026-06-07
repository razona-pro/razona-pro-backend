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

    /**
     * Ranking GENERAL (todas las competencias): suma el score de los intentos EXAM/TIMED
     * FINISHED en el período [start,end] (null = sin límite).
     */
    @Query("""
        SELECT COALESCE(SUM(t.score), 0), COUNT(t)
        FROM Tried t, com.razonapro.razonaprobackend.domain.test.model.Test te
        WHERE te.testId = t.testId
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
     * Ranking POR COMPETENCIAS (una o varias): suma los puntos PONDERADOS de las respuestas
     * correctas de ESAS competencias dentro de intentos EXAM/TIMED FINISHED. El conteo es de
     * intentos DISTINTOS (no se duplican aunque abarquen varias competencias del conjunto).
     * El peso por dificultad coincide con el motor de puntaje (N/A/B=1, M=2, A=3).
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN q.difficultyLevel = 'A' THEN 3
                                 WHEN q.difficultyLevel = 'M' THEN 2
                                 ELSE 1 END), 0),
               COUNT(DISTINCT sr.triedId)
        FROM com.razonapro.razonaprobackend.domain.tried.model.StudentResponse sr,
             Tried t,
             com.razonapro.razonaprobackend.domain.test.model.Test te,
             com.razonapro.razonaprobackend.domain.question.model.Question q
        WHERE sr.triedId = t.triedId AND sr.studentId = t.studentId AND sr.programId = t.programId
          AND sr.studentId = :studentId AND sr.programId = :programId
          AND sr.isCorrect = true AND sr.optionId IS NOT NULL
          AND sr.competenceId IN :competenceIds
          AND q.competenceId = sr.competenceId AND q.questionId = sr.questionId
          AND te.testId = t.testId AND te.testMode IN ('EXAM','TIMED')
          AND t.status = 'FINISHED'
          AND (:start IS NULL OR COALESCE(t.finishedAt, t.attemptTimestamp) >= :start)
          AND (:end   IS NULL OR COALESCE(t.finishedAt, t.attemptTimestamp) <= :end)
    """)
    java.util.List<Object[]> sumTriedsByCompetencesForRanking(@Param("studentId") String studentId,
                                                              @Param("programId") String programId,
                                                              @Param("competenceIds") java.util.Collection<String> competenceIds,
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

}