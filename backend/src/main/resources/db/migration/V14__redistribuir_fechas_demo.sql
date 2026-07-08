-- V14: Redistribuye las fechas de los eventos de demo/seed para que todos
-- caigan 2+ meses en el futuro respecto a julio 2026, variadas entre sí,
-- para que la cartelera se vea "viva" durante demostraciones del proyecto.
-- No afecta título, estado ni zona — solo event_date.

UPDATE events SET event_date = '2026-09-18 18:00:00' WHERE id = '22222222-2222-2222-2222-222222222222'; -- Concierto Sinfónico y Coro UCSG
UPDATE events SET event_date = '2026-09-25 15:00:00' WHERE id = '33333333-3333-3333-3333-333333333333'; -- Copa Interfacultades de Fútbol
UPDATE events SET event_date = '2026-10-08 09:00:00' WHERE id = '44444444-4444-4444-4444-444444444444'; -- Congreso de Ingeniería e IA
UPDATE events SET event_date = '2026-09-10 10:00:00' WHERE id = '11111111-1111-1111-1111-111111111111'; -- Casa Abierta de Medicina
UPDATE events SET event_date = '2026-09-12 08:00:00' WHERE id = '9c9ee876-28b4-4e26-a80e-c3fcd311f4e5'; -- ¡Tu voz cuenta!
UPDATE events SET event_date = '2026-09-28 19:00:00' WHERE id = 'f4932ab8-a6e9-4a52-ab56-967f043314a0'; -- Vive la emoción del Mundial
UPDATE events SET event_date = '2026-10-25 08:00:00' WHERE id = 'b0e1f5bf-439f-4599-ad5c-c13bd9aababc'; -- Faltan 2 días Cato Fan Fest
UPDATE events SET event_date = '2026-10-12 08:00:00' WHERE id = '69647206-42aa-4a7c-a018-a2c109ae847b'; -- Feria Laboral Conexiones
UPDATE events SET event_date = '2026-11-03 10:00:00' WHERE id = '82afa83e-7c34-4cce-8559-9ecfe1d9f1c5'; -- Legado cañari
UPDATE events SET event_date = '2026-11-12 09:00:00' WHERE id = 'aad2db79-2104-4795-a117-7a511df4742c'; -- Tu historia universitaria
UPDATE events SET event_date = '2026-10-15 08:30:00' WHERE id = 'dd11f507-ba30-4cc4-9e6a-55efc8574b9c'; -- Las grandes obras empiezan
UPDATE events SET event_date = '2026-09-30 16:00:00' WHERE id = 'e1f4f732-8949-442a-b8f4-de625ac43835'; -- No hay duda barra ecuatoriana
UPDATE events SET event_date = '2026-10-18 08:00:00' WHERE id = 'a876b5d9-8dce-42ab-b478-980a848a20bf'; -- Trabajas horarios exigentes
UPDATE events SET event_date = '2026-09-22 08:00:00' WHERE id = '99b5c88d-a06a-4616-8a7a-62be6fee56d3'; -- Tu aprendizaje en la UCSG
UPDATE events SET event_date = '2026-11-05 17:00:00' WHERE id = '7f17da1e-407f-4cc4-b923-185ae84a1c8e'; -- Tu próxima experiencia internacional
UPDATE events SET event_date = '2026-10-20 14:00:00' WHERE id = 'f7ee0f7a-3aca-4273-947d-1c428ff87c40'; -- Las ideas ya están cargadas
UPDATE events SET event_date = '2026-09-15 12:30:00' WHERE id = '1e0d505f-e211-4533-a1ba-47810a537175'; -- Ese cromo que te falta
UPDATE events SET event_date = '2026-11-20 16:00:00' WHERE id = '6ad09ee4-b8a6-40bf-99c0-2540a9942398'; -- Al igual que tú Mundial 2026
UPDATE events SET event_date = '2026-10-01 08:00:00' WHERE id = 'd7278315-b620-44c6-aee4-db886919a1cd'; -- Las grandes oportunidades
UPDATE events SET event_date = '2026-09-08 09:00:00' WHERE id = '049f8ec3-dfe8-4063-acd8-18d8fa4a380f'; -- Conoce el cronograma elecciones
