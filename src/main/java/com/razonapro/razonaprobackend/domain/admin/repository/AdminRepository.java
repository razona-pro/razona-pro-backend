package com.razonapro.razonaprobackend.domain.admin.repository;

import com.razonapro.razonaprobackend.domain.admin.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    long countByIsActiveTrue();
    long countByIsActiveFalse();

    @org.springframework.data.jpa.repository.Query("""
        SELECT a FROM Admin a
        WHERE (:search IS NULL OR
               LOWER(a.adminId)      LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(a.firstName)    LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(a.firstSurname) LIKE LOWER(CONCAT('%',:search,'%')) OR
               LOWER(a.email)        LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:statusFilter = '' OR
               (:statusFilter = 'active'   AND a.isActive = true)  OR
               (:statusFilter = 'inactive' AND a.isActive = false))
        ORDER BY a.createdAt DESC
    """)
    org.springframework.data.domain.Page<Admin> findByFilters(
        @org.springframework.data.repository.query.Param("search")       String search,
        @org.springframework.data.repository.query.Param("statusFilter") String statusFilter,
        org.springframework.data.domain.Pageable pageable
    );
}