package com.razonapro.razonaprobackend.domain.student.repository;

import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.shared.ids.StudentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, StudentId> {

    Optional<Student> findByEmail(String email);
    Optional<Student> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    long countByIsActiveTrue();
    List<Student> findByIsActiveTrue();
}