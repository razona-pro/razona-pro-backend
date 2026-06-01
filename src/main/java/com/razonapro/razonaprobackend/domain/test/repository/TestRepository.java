package com.razonapro.razonaprobackend.domain.test.repository;

import com.razonapro.razonaprobackend.domain.test.model.Test;
import com.razonapro.razonaprobackend.shared.ids.TestPK;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends JpaRepository<Test, TestPK> {

    @Query(value = "SELECT t FROM Test t JOIN FETCH t.competence WHERE t.isActive = true",
            countQuery = "SELECT COUNT(t) FROM Test t WHERE t.isActive = true")
    Page<Test> findAllActiveWithCompetence(Pageable pageable);

    @Query(value = "SELECT t FROM Test t JOIN FETCH t.competence",
            countQuery = "SELECT COUNT(t) FROM Test t")
    Page<Test> findAllWithCompetence(Pageable pageable);

    /** Verificar nombre único por competencia (para crear) */
    boolean existsByTestNameIgnoreCaseAndCompetenceId(String testName, String competenceId);

    /** Verificar nombre único por competencia excluyendo el propio (para editar) */
    boolean existsByTestNameIgnoreCaseAndCompetenceIdAndTestIdNot(
            String testName, String competenceId, String testId);
}