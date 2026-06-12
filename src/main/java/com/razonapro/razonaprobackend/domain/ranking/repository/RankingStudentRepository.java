package com.razonapro.razonaprobackend.domain.ranking.repository;

import com.razonapro.razonaprobackend.domain.ranking.model.RankingStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RankingStudentRepository extends JpaRepository<RankingStudent, Integer> {

    // Solo aparecen estudiantes con actividad REAL en este ranking. Cada finalización crea/actualiza
    // una fila en TODOS los rankings activos, así que sin este filtro salían estudiantes con
    // "0 pruebas" (p. ej. quien solo hizo IA en un ranking de solo-pruebas).
    @Query(value = """
        SELECT rs FROM RankingStudent rs
        JOIN FETCH rs.student s
        WHERE rs.ranking.rankingId = :rankingId
          AND (COALESCE(rs.triedsCount, 0) + COALESCE(rs.aiTriedsCount, 0)) > 0
        ORDER BY rs.totalScore DESC
        """,
            countQuery = """
        SELECT COUNT(rs) FROM RankingStudent rs
        WHERE rs.ranking.rankingId = :rankingId
          AND (COALESCE(rs.triedsCount, 0) + COALESCE(rs.aiTriedsCount, 0)) > 0
        """)
    Page<RankingStudent> findLeaderboard(String rankingId, Pageable pageable);

    boolean existsByRankingRankingIdAndStudentIdAndProgramId(
            String rankingId, String studentId, String programId);

    /** Filas de un estudiante en un ranking (todos los períodos); el período se filtra en memoria. */
    java.util.List<RankingStudent> findByRankingRankingIdAndStudentIdAndProgramId(
            String rankingId, String studentId, String programId);
}