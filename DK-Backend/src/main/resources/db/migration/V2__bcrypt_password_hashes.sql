-- Migración de seguridad: hashes BCrypt compatibles con Spring Security (cost 12).
-- Contraseña temporal de los usuarios seed: changeme
-- OBLIGATORIO: cambiar contraseñas antes de desplegar a producción.

UPDATE usuario SET password_hash = '$2a$12$IAJNvqUq79VlZm.PL6UA/.vH1Gn.CzqnwTe94htOU2Cjd0VLw1cI6'
WHERE email IN ('kevin@dk.cl', 'daniel@dk.cl', 'arnely@dk.cl');
