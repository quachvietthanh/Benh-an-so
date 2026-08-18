-- Add MANAGER role for clinic operational reporting and invoice adjustment.

INSERT INTO roles (id, name, description, is_system, created_at, updated_at) VALUES
(UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), 'MANAGER', 'Clinic manager', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission) VALUES
(UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), 'INVOICE_READ'),
(UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), 'INVOICE_UPDATE'),
(UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), 'AUDIT_READ'),
(UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), 'REPORT_VIEW'),
(UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), 'REPORT_EXPORT');

INSERT INTO users (id, username, password_hash, full_name, email, phone, role_id, active, last_login_at, created_at) VALUES
(UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7'), 'manager1', '$2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK', 'Clinic Manager', 'manager1@benhsoan.com', '0901000006', UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), TRUE, NULL, CURRENT_TIMESTAMP);
