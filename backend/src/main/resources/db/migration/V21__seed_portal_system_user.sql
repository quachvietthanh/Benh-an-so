-- =====================================================
-- V22__seed_portal_system_user.sql
-- Technical "system" account used as the audit actor for
-- anonymous patient-portal lookups (NCL-10-CN-003 / QTN-15).
-- Inactive so it can never be used to log in.
-- =====================================================

INSERT INTO users (id, username, password_hash, full_name, email, phone, role_id, active, last_login_at, created_at)
SELECT UUID_TO_BIN('00000000-0000-0000-0000-000000000000'),
       'system',
       '$2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK',
       'System',
       'system@benhsoan.com',
       NULL,
       roles.id,
       FALSE,
       NULL,
       CURRENT_TIMESTAMP
FROM roles
WHERE roles.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM users
      WHERE users.id = UUID_TO_BIN('00000000-0000-0000-0000-000000000000')
  );
