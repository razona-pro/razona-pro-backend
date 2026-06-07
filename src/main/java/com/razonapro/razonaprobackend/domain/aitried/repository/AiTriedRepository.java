package com.razonapro.razonaprobackend.domain.aitried.repository;

import com.razonapro.razonaprobackend.domain.aitried.model.AiTried;
import com.razonapro.razonaprobackend.shared.ids.AiTriedId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiTriedRepository extends JpaRepository<AiTried, AiTriedId> {
    Page<AiTried> findByStudentIdAndProgramId(String studentId, String programId, Pageable pageable);
    Optional<AiTried> findByAiTriedId(String aiTriedId);

    /** Historial admin: todas las sesiones IA, con filtro opcional por estudiante. */
    @Query("""
        SELECT a FROM AiTried a
        WHERE (:studentId IS NULL OR a.studentId = :studentId)
    """)
    Page<AiTried> findForAdmin(@Param("studentId") String studentId, Pageable pageable);

    /**
     * Suma de score y conteo para ranking: sesiones IA FINISHED en el período [start,end]
     * (null = sin límite). Si competenceId no es null, solo cuenta esa competencia.
     */
    @Query("""
        SELECT COALESCE(SUM(a.score), 0), COUNT(a)
        FROM AiTried a
        WHERE a.studentId = :studentId AND a.programId = :programId
          AND a.status = 'FINISHED' AND a.score IS NOT NULL
          AND (:competenceId IS NULL OR a.competenceId = :competenceId)
          AND (:start IS NULL OR COALESCE(a.finishedAt, a.attemptTimestamp) >= :start)
          AND (:end   IS NULL OR COALESCE(a.finishedAt, a.attemptTimestamp) <= :end)
    """)
    java.util.List<Object[]> sumAiForRanking(@Param("studentId") String studentId,
                                             @Param("programId") String programId,
                                             @Param("competenceId") String competenceId,
                                             @Param("start") java.time.LocalDateTime start,
                                             @Param("end")   java.time.LocalDateTime end);

    /**
     * Suma de score y conteo para ranking POR COMPETENCIAS (una o varias): sesiones IA FINISHED
     * de ESAS competencias en el período [start,end] (null = sin límite).
     */
    @Query("""
        SELECT COALESCE(SUM(a.score), 0), COUNT(a)
        FROM AiTried a
        WHERE a.studentId = :studentId AND a.programId = :programId
          AND a.status = 'FINISHED' AND a.score IS NOT NULL
          AND a.competenceId IN :competenceIds
          AND (:start IS NULL OR COALESCE(a.finishedAt, a.attemptTimestamp) >= :start)
          AND (:end   IS NULL OR COALESCE(a.finishedAt, a.attemptTimestamp) <= :end)
    """)
    java.util.List<Object[]> sumAiByCompetencesForRanking(@Param("studentId") String studentId,
                                                          @Param("programId") String programId,
                                                          @Param("competenceIds") java.util.Collection<String> competenceIds,
                                                          @Param("start") java.time.LocalDateTime start,
                                                          @Param("end")   java.time.LocalDateTime end);
}