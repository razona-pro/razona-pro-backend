package com.razonapro.razonaprobackend.models;

import com.razonapro.razonaprobackend.models.ids.AiTriedResponseId;
import com.razonapro.razonaprobackend.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "ai_tried_responses", schema = "razonapro")
@IdClass(AiTriedResponseId.class)
public class AiTriedResponse {

    @Id @Column(name = "program_id",            length = 3)  private String programId;
    @Id @Column(name = "student_id",            length = 7)  private String studentId;
    @Id @Column(name = "ai_tried_id",           length = 10) private String aiTriedId;
    @Id @Column(name = "ai_tried_response_id",  length = 10) private String aiTriedResponseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "program_id",  referencedColumnName = "program_id",  insertable = false, updatable = false),
        @JoinColumn(name = "student_id",  referencedColumnName = "student_id",  insertable = false, updatable = false),
        @JoinColumn(name = "ai_tried_id", referencedColumnName = "ai_tried_id", insertable = false, updatable = false)
    })
    private AiTried aiTried;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competence_id")
    private Competence competence;

    @Column(name = "question_text",  length = 300, nullable = false) private String questionText;
    @Column(name = "student_answer", length = 200, nullable = false) private String studentAnswer;
    @Column(name = "correct_answer", length = 200, nullable = false) private String correctAnswer;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "is_correct", columnDefinition = "CHAR(1)", nullable = false)
    private Boolean isCorrect;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
