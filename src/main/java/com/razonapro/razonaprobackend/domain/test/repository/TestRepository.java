package com.razonapro.razonaprobackend.domain.test.repository;

import com.razonapro.razonaprobackend.domain.test.model.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, String> {

    /** Carga la prueba por su id (la identidad de la prueba). */
    Optional<Test> findByTestId(String testId);

    Page<Test> findByIsActiveTrue(Pageable pageable);

    /** Nombre de prueba único (las pruebas ya no se separan por competencia). */
    boolean existsByTestNameIgnoreCase(String testName);
    boolean existsByTestNameIgnoreCaseAndTestIdNot(String testName, String testId);
}
