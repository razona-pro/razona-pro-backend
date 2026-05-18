package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.StudentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentResponseRepository extends JpaRepository<StudentResponse, String> {
    List<StudentResponse> findByTriedId(String triedId);
    boolean existsByTriedIdAndQuestionId(String triedId, String questionId);
    long countByTriedIdAndIsCorrectTrue(String triedId);
}
