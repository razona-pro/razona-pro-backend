package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, String> {
    List<Ranking> findByIsActiveTrue();
    long count();
}
