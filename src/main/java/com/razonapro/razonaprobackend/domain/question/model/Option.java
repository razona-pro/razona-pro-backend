package com.razonapro.razonaprobackend.domain.question.model;

import com.razonapro.razonaprobackend.shared.ids.OptionId;
import com.razonapro.razonaprobackend.infrastructure.util.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "options", schema = "razonapro")
@IdClass(OptionId.class)
public class Option {

    @Id
    @Column(name = "competence_id", length = 6)
    private String competenceId;

    @Id
    @Column(name = "question_id", length = 7)
    private String questionId;

    @Id
    @Column(name = "option_id", length = 6)
    private String optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "competence_id", referencedColumnName = "competence_id", insertable = false, updatable = false),
        @JoinColumn(name = "question_id",   referencedColumnName = "question_id",   insertable = false, updatable = false)
    })
    private Question question;

    @Column(name = "option_text", length = 300, nullable = false)
    private String optionText;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "is_correct", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;
}
