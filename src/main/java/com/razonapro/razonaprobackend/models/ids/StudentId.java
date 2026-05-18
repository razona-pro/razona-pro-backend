package com.razonapro.razonaprobackend.models.ids;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class StudentId implements Serializable {
    private String studentId;
    private String programId;
}
