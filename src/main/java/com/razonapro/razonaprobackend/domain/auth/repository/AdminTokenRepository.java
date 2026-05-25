package com.razonapro.razonaprobackend.domain.auth.repository;

import com.razonapro.razonaprobackend.domain.auth.model.AdminToken;
import com.razonapro.razonaprobackend.domain.auth.model.enums.AdminTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminTokenRepository extends JpaRepository<AdminToken, Long> {

    Optional<AdminToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE AdminToken t SET t.usedAt = CURRENT_TIMESTAMP " +
            "WHERE t.adminId = :adminId AND t.tokenType = :type AND t.usedAt IS NULL")
    void invalidateAllByAdminAndType(String adminId, AdminTokenType type);
}