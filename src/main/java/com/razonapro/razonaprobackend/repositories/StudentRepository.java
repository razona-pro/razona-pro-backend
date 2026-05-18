package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.Student;
import com.razonapro.razonaprobackend.models.ids.StudentId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, StudentId> {
    Optional<Student> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Page<Student> findByProgramId(String programId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.programId = :programId")
    long countByProgramId(String programId);

    @Query("SELECT COUNT(s) FROM Student s")
    long countAll();
}
