package com.razonapro.razonaprobackend.shared.ids;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class TriedId implements Serializable {
    private String competenceId;
    private String testId;
    private String programId;
    private String studentId;
    private String triedId;
}