package com.razonapro.razonaprobackend.shared.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class NormalizingEntityListener {

    @PrePersist
    @PreUpdate
    public void normalize(Object entity) {
        if (entity instanceof Normalizable n) {
            n.normalize();
        }
    }
}