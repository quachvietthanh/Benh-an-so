package com.benhsoan.persistence.entity.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttemptEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 100)
    private String identifier;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
