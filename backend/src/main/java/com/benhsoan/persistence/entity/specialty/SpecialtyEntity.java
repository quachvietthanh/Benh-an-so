package com.benhsoan.persistence.entity.specialty;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "specialties")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(nullable = false, length = 30)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "name_key", nullable = false, length = 100)
    private String nameKey;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void syncNameKey() {
        if (this.name != null && (this.nameKey == null || this.nameKey.isBlank())) {
            this.nameKey = this.name.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
