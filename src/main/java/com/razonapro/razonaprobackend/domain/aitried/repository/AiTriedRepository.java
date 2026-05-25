package com.razonapro.razonaprobackend.domain.aitried.repository;

import com.razonapro.razonaprobackend.domain.aitried.model.AiTried;
import com.razonapro.razonaprobackend.shared.ids.AiTriedId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AiTriedRepository extends JpaRepository<AiTried, AiTriedId> {
    Page<AiTried> findByStudentIdAndProgramId(String studentId, String programId, Pageable pageable);
    Optional<AiTried> findByAiTriedId(String aiTriedId);
}
