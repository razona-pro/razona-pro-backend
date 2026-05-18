package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.RankingStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RankingStudentRepository extends JpaRepository<RankingStudent, Integer> {

    @Query("SELECT rs FROM RankingStudent rs " +
           "JOIN FETCH rs.student s " +
           "WHERE rs.ranking.rankingId = :rankingId " +
           "ORDER BY rs.totalScore DESC")
    Page<RankingStudent> findLeaderboard(String rankingId, Pageable pageable);

    boolean existsByRankingRankingIdAndStudentIdAndProgramId(
        String rankingId, String studentId, String programId);
}
