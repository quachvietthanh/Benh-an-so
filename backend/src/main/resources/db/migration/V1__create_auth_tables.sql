-- =====================================================
-- V1__create_auth_tables.sql
-- Authentication & Authorization Schema
-- =====================================================

-- ===========================
-- Roles
-- ===========================

CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

-- ===========================
-- Role Permissions
-- ===========================

CREATE TABLE role_permissions (
    role_id BINARY(16) NOT NULL,
    permission VARCHAR(100) NOT NULL,

    CONSTRAINT pk_role_permissions
        PRIMARY KEY (role_id, permission),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);

-- ===========================
-- Users
-- ===========================

CREATE TABLE users (
    id BINARY(16) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role_id BINARY(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
);

-- ===========================
-- User Sessions
-- ===========================

CREATE TABLE user_sessions (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    previous_refresh_token_hash VARCHAR(255) NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,

    CONSTRAINT pk_user_sessions PRIMARY KEY (id),

    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- ===========================
-- Audit Logs
-- ===========================

CREATE TABLE audit_logs (
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    action_type VARCHAR(30) NOT NULL,

    resource_type VARCHAR(30) NOT NULL,

    resource_id BINARY(16),

    detail JSON,

    ip_address VARCHAR(45),

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_audit_logs
        PRIMARY KEY (id),

    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

-- =====================================================
-- Indexes
-- =====================================================

CREATE INDEX idx_users_role
    ON users(role_id);

CREATE INDEX idx_role_permissions_permission
    ON role_permissions(permission);

CREATE UNIQUE INDEX uk_user_sessions_refresh_token_hash
    ON user_sessions(refresh_token_hash);

CREATE INDEX idx_user_sessions_user_expires
    ON user_sessions(user_id, refresh_expires_at);

CREATE INDEX idx_user_sessions_previous_refresh_token_hash
    ON user_sessions(previous_refresh_token_hash);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs(created_at);

CREATE INDEX idx_audit_logs_resource
    ON audit_logs(resource_type, resource_id);

CREATE INDEX idx_audit_logs_action
    ON audit_logs(action_type);

CREATE INDEX idx_audit_logs_user_created
    ON audit_logs(user_id, created_at);
