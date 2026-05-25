package com.razonapro.razonaprobackend.domain.ranking.repository;

import com.razonapro.razonaprobackend.domain.ranking.model.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, String> {
    List<Ranking> findByIsActiveTrue();
    long count();
}
