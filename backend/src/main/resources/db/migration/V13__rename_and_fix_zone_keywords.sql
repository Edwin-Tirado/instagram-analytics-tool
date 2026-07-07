-- ─────────────────────────────────────────────────────────────────────────────
-- V13: Renombra una zona y refuerza match_keywords de otras dos.
--
-- 1. "Auditorio de Ingeniería - Piso 3" → "Auditorio Facultad Ingeniería Civil"
--    (pedido explícito del usuario). Es un UPDATE de nombre, no un
--    DELETE+INSERT — no afecta a los eventos que ya tuvieran esta zona
--    asignada (siguen apuntando al mismo id).
--
-- 2. "Auditorio Félix Henríquez": el docx original traía "Auditorio Felix
--    Henriques" (con "s", sin tilde). Se agregó esa variante de ortografía
--    a match_keywords por si el caption real de Instagram usa esa forma en
--    vez de "Henríquez" — antes solo esa variante hacía match.
--
-- 3. "Plaza Qatar": ya viene matcheando bien (verificado con 3 eventos
--    reales), se deja igual.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE zones SET name = 'Auditorio Facultad Ingeniería Civil'
    WHERE name = 'Auditorio de Ingeniería - Piso 3';

UPDATE zones SET match_keywords = 'auditorio felix henriquez,felix henriquez,auditorio felix henriques,felix henriques'
    WHERE name = 'Auditorio Félix Henríquez';
