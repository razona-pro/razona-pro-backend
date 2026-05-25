package com.razonapro.razonaprobackend.domain.test.repository;

import com.razonapro.razonaprobackend.domain.test.model.Test;
import com.razonapro.razonaprobackend.shared.ids.TestPK;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, TestPK> {
    List<Test> findByCompetenceIdAndIsActiveTrue(String competenceId);
    Page<Test> findByCompetenceId(String competenceId, Pageable pageable);
    long count();
}
