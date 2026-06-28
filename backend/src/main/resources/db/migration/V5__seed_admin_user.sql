-- V5: Usuario administrador para desarrollo
-- Contraseña: Admin@ucsg2026  (BCrypt, cost=12)
-- Cambiar antes de producción

INSERT INTO users (id, email, password, full_name, enabled)
VALUES (
    gen_random_uuid(),
    'admin@cu.ucsg.edu.ec',
    '$2a$12$9wHiRWQaGnRGOx6DPM.FkOA1VuGJ3L3EVnnIBqEfFNE7Z8u0dCqry',
    'Administrador UCSG',
    true
)
ON CONFLICT (email) DO NOTHING;

-- Asignar rol ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@cu.ucsg.edu.ec'
  AND r.name  = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Usuario supervisor para pruebas
-- Contraseña: Super@ucsg2026  (BCrypt, cost=12)
INSERT INTO users (id, email, password, full_name, enabled)
VALUES (
    gen_random_uuid(),
    'supervisor@cu.ucsg.edu.ec',
    '$2a$12$sXqvxKmTn.mPLs4n.I7vKuBrIkFH.PGh.H6i7R.bRSMCDKRfLY6ey',
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
