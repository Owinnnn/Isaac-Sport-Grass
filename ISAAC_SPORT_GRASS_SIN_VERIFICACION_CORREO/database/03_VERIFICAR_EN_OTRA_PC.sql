-- SOLO CONSULTAS. NO CREA, BORRA NI MODIFICA DATOS.
-- Ejecutar DESPUES de importar tu archivo isaac.sql en la otra computadora.
SELECT DATABASE() AS base_actual;
SELECT TABLE_NAME AS tabla, TABLE_ROWS AS filas_estimadas
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'isaac_sport_grass' AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
SELECT COUNT(*) AS usuarios FROM isaac_sport_grass.users;
SELECT COUNT(*) AS canchas FROM isaac_sport_grass.courts;
SELECT COUNT(*) AS reservas FROM isaac_sport_grass.reservations;
SELECT COUNT(*) AS mensajes_soporte FROM isaac_sport_grass.support_messages;
SELECT COUNT(*) AS configuraciones_pago FROM isaac_sport_grass.payment_settings;
