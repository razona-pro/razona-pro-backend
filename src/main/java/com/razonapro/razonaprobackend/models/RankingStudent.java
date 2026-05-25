package com.razonapro.razonaprobackend.models;

import com.razonapro.razonaprobackend.domain.ranking.model.Ranking;
import com.razonapro.razonaprobackend.domain.student.model.Student;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "rankings_students", schema = "razonapro")
public class RankingStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "seq_ranking_student")
    @SequenceGenerator(name = "seq_ranking_student",
                       sequenceName = "razonapro.seq_ranking_student_id",
                       allocationSize = 1)
    @Column(name = "ranking_student_id")
    private Integer rankingStudentId;

    @Column(name = "program_id", length = 3, nullable = false)
    private String programId;

    @Column(name = "student_id", length = 7, nullable = false)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "student_id", referencedColumnName = "student_id", insertable = false, updatable = false),
        @JoinColumn(name = "program_id", referencedColumnName = "program_id", insertable = false, updatable = false)
    })
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranking_id")
    private Ranking ranking;

    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end")   private LocalDate periodEnd;

    @Column(name = "total_score",     precision = 8, scale = 2, nullable = false)
    @Builder.Default private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(name = "trieds_score",    precision = 7, scale = 2, nullable = false)
    @Builder.Default private BigDecimal triedsScore = BigDecimal.ZERO;

    @Column(name = "ai_trieds_score", precision = 7, scale = 2, nullable = false)
    @Builder.Default private BigDecimal aiTriedsScore = BigDecimal.ZERO;

    @Column(name = "trieds_count",    nullable = false)
    @Builder.Default private Integer triedsCount = 0;

    @Column(name = "ai_trieds_count", nullable = false)
    @Builder.Default private Integer aiTriedsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_activity_at") private LocalDateTime lastActivityAt;
    @Column(name = "updated_at")       private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
