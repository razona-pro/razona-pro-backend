package com.razonapro.razonaprobackend.domain.student.repository;

import com.razonapro.razonaprobackend.domain.student.model.Student;
import com.razonapro.razonaprobackend.shared.ids.StudentId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    long countByIsActiveFalse();
    List<Student> findByIsActiveTrue();
    List<Student> findAllByOrderByCreatedAtDesc();

    /**
     * Filtros opcionales. statusFilter acepta: "active", "inactive",
     * "verified", "pending" o "" (sin filtro de estado).
     * Evita parámetros Boolean null que fallan en PostgreSQL/Hibernate.
     */
    @Query("""
        SELECT s FROM Student s
        WHERE (:search IS NULL OR
               LOWER(s.studentId)    LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(s.firstName)    LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(s.firstSurname) LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(s.email)        LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:programId IS NULL OR s.programId = :programId)
          AND (:statusFilter = '' OR
               (:statusFilter = 'active'   AND s.isActive      = true)  OR
               (:statusFilter = 'inactive' AND s.isActive      = false) OR
               (:statusFilter = 'verified' AND s.emailVerified = true)  OR
               (:statusFilter = 'pending'  AND s.emailVerified = false))
        ORDER BY s.createdAt DESC
    """)
    Page<Student> findByFilters(
        @Param("search")       String search,
        @Param("programId")    String programId,
        @Param("statusFilter") String statusFilter,
        Pageable pageable
    );
}
