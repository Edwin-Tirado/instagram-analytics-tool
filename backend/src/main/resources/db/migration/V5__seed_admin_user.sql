-- V5: Usuario administrador para desarrollo
-- Las contraseñas se establecen vía procedimiento al arrancar,
-- pero insertamos el usuario con un hash conocido-válido generado con BCrypt(12).
--
-- Admin:      Admin@ucsg2026
-- Supervisor: Super@ucsg2026
--
-- Hash generado con: new BCryptPasswordEncoder(12).encode("Admin@ucsg2026")
-- Verificable en: https://bcrypt-generator.com  (rounds=12)

INSERT INTO users (id, email, password, full_name, enabled)
VALUES (
    gen_random_uuid(),
    'admin@cu.ucsg.edu.ec',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMqJqhN3HDY5fL5.oFV4P4KR.2',
    'Administrador UCSG',
    true
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@cu.ucsg.edu.ec'
  AND r.name  = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO users (id, email, password, full_name, enabled)
VALUES (
    gen_random_uuid(),
    'supervisor@cu.ucsg.edu.ec',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMqJqhN3HDY5fL5.oFV4P4KR.2',
    'Supervisor UCSG',
    true
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'supervisor@cu.ucsg.edu.ec'
  AND r.name  = 'ROLE_SUPERVISOR'
ON CONFLICT DO NOTHING;
