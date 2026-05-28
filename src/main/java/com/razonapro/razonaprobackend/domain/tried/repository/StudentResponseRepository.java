package com.razonapro.razonaprobackend.domain.tried.repository;

import com.razonapro.razonaprobackend.domain.tried.model.StudentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentResponseRepository extends JpaRepository<StudentResponse, String> {

    List<StudentResponse> findByTriedIdAndOptionIdIsNotNull(String triedId);

    Optional<StudentResponse> findByTriedIdAndQuestionId(String triedId, String questionId);

    boolean existsByTriedIdAndQuestionId(String triedId, String questionId);

    long countByTriedIdAndIsCorrectTrueAndOptionIdIsNotNull(String triedId);

    // findByTriedId sigue igual
    List<StudentResponse> findByTriedId(String triedId);
}