package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.AdminToken;
import com.razonapro.razonaprobackend.models.enums.AdminTokenType;
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