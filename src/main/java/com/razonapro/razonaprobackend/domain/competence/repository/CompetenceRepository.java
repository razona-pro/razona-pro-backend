package com.razonapro.razonaprobackend.domain.competence.repository;

import com.razonapro.razonaprobackend.domain.competence.model.Competence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CompetenceRepository extends JpaRepository<Competence, String> {
    List<Competence> findByIsActiveTrue();
}
