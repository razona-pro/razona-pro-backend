package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
