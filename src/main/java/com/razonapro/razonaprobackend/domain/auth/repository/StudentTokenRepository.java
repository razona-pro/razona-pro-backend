package com.razonapro.razonaprobackend.domain.auth.repository;

import com.razonapro.razonaprobackend.domain.auth.model.StudentToken;
import com.razonapro.razonaprobackend.domain.auth.model.enums.StudentTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentTokenRepository extends JpaRepository<StudentToken, Long> {

    Optional<StudentToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE StudentToken t SET t.usedAt = CURRENT_TIMESTAMP " +
            "WHERE t.studentId = :studentId AND t.tokenType = :type AND t.usedAt IS NULL")
    void invalidateAllByStudentAndType(String studentId, StudentTokenType type);
}