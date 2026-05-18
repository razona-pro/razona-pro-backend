package com.razonapro.razonaprobackend.repositories;

import com.razonapro.razonaprobackend.models.Tried;
import com.razonapro.razonaprobackend.models.ids.TriedId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TriedRepository extends JpaRepository<Tried, TriedId> {

    List<Tried> findByStudentIdAndProgramIdOrderByAttemptTimestampDesc(String studentId, String programId);

    Page<Tried> findByStudentIdAndProgramId(String studentId, String programId, Pageable pageable);

    Optional<Tried> findByTriedId(String triedId);

    @Query("SELECT t FROM Tried t WHERE t.studentId = :studentId AND t.programId = :programId AND t.status = 'IN_PROGRESS'")
    List<Tried> findInProgressByStudent(String studentId, String programId);

    boolean existsByTriedId(String triedId);
}
