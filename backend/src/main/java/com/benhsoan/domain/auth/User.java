package com.benhsoan.domain.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString(exclude = "passwordHash")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private UUID id;

    private String username;

    private String passwordHash;

    private String fullName;

    private String email;

    private String phone;

    private UUID roleId;

    private boolean active;

    private boolean mustChangePassword;

    private Instant lastLoginAt;

    private Instant createdAt;

    private User(
            UUID id,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UUID roleId,
            boolean active,
            boolean mustChangePassword,
            Instant lastLoginAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.username = Guard.require(username, "Username");
        this.passwordHash = Guard.require(passwordHash, "Password");
        this.fullName = Guard.require(fullName, "Full name");
        this.email = Guard.require(email, "Email");
        this.phone = phone;
        this.roleId = Objects.requireNonNull(roleId);
        this.active = active;
        this.mustChangePassword = mustChangePassword;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static User create(
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UUID roleId
    ) {
        return new User(
                UUID.randomUUID(),
                username,
                passwordHash,
                fullName,
                email,
                phone,
                roleId,
                true,
                false,
                null,
                Instant.now()
        );
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Guard.require(newPasswordHash, "Password");
        this.mustChangePassword = false;
    }

    public void resetPassword(String tempPasswordHash) {
        this.passwordHash = Guard.require(tempPasswordHash, "Password");
        this.mustChangePassword = true;
    }

    public void updateProfile(
            String fullName,
            String email,
            String phone
    ) {
        this.fullName = Guard.require(fullName, "Full name");
        this.email = Guard.require(email, "Email");
        this.phone = phone;
    }

    public void updateLastLogin(Instant loginTime) {
        this.lastLoginAt = Guard.require(loginTime, "Login time");
    }


    public static User restore(
            UUID id,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UUID roleId,
            boolean active,
            Instant lastLoginAt,
            Instant createdAt
    ) {
        return restore(
                id,
                username,
                passwordHash,
                fullName,
                email,
                phone,
                roleId,
                active,
                false,
                lastLoginAt,
                createdAt
        );
    }

    public static User restore(
            UUID id,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UUID roleId,
            boolean active,
            boolean mustChangePassword,
            Instant lastLoginAt,
            Instant createdAt
    ) {
        return new User(
                id,
                username,
                passwordHash,
                fullName,
                email,
                phone,
                roleId,
                active,
                mustChangePassword,
                lastLoginAt,
                createdAt
        );
    }
}