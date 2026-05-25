package com.razonapro.razonaprobackend.domain.question.model;

import com.razonapro.razonaprobackend.shared.ids.OptionId;
import com.razonapro.razonaprobackend.shared.jpa.Normalizable;
import com.razonapro.razonaprobackend.shared.jpa.NormalizingEntityListener;
import com.razonapro.razonaprobackend.shared.util.StringNormalizer;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "options")
@IdClass(OptionId.class)
@EntityListeners(NormalizingEntityListener.class)
public class Option implements Normalizable {

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

    @Column(name = "is_correct", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Override
    public void normalize() {
        optionText = StringNormalizer.trim(optionText);
    }
}