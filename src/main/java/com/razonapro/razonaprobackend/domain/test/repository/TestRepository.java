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

    @Query("SELECT t FROM Test t JOIN FETCH t.competence WHERE t.isActive = true")
    Page<Test> findAllActiveWithCompetence(Pageable pageable);

    @Query("SELECT t FROM Test t JOIN FETCH t.competence")
    Page<Test> findAllWithCompetence(Pageable pageable);
}