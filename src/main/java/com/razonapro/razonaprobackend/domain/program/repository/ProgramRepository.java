package com.razonapro.razonaprobackend.domain.program.repository;

import com.razonapro.razonaprobackend.domain.program.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, String> {
    List<Program> findByIsActiveTrue();
}
