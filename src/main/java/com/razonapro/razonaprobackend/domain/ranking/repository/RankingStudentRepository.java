package com.razonapro.razonaprobackend.domain.ranking.repository;

import com.razonapro.razonaprobackend.domain.ranking.model.RankingStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RankingStudentRepository extends JpaRepository<RankingStudent, Integer> {

    @Query(value = """
        SELECT rs FROM RankingStudent rs
        JOIN FETCH rs.student s
        WHERE rs.ranking.rankingId = :rankingId
        ORDER BY rs.totalScore DESC
        """,
            countQuery = "SELECT COUNT(rs) FROM RankingStudent rs WHERE rs.ranking.rankingId = :rankingId")
    Page<RankingStudent> findLeaderboard(String rankingId, Pageable pageable);

    boolean existsByRankingRankingIdAndStudentIdAndProgramId(
            String rankingId, String studentId, String programId);

    /** Filas de un estudiante en un ranking (todos los períodos); el período se filtra en memoria. */
    java.util.List<RankingStudent> findByRankingRankingIdAndStudentIdAndProgramId(
            String rankingId, String studentId, String programId);
}