package com.razonapro.razonaprobackend.shared.ids;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class StudentId implements Serializable {
    private String studentId;
    private String programId;
}