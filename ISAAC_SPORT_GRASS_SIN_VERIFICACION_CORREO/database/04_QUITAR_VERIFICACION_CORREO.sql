-- Ejecutar solo si se desea normalizar cuentas anteriores en una base existente.
-- No borra usuarios, contraseñas, reservas ni mensajes.
USE isaac_sport_grass;
UPDATE users SET verified=1, verify_token_hash=NULL, verify_expires=NULL WHERE verified=0;
-- Opcional: para nuevas inserciones SQL que omitan verified
ALTER TABLE users MODIFY COLUMN verified BOOLEAN NOT NULL DEFAULT TRUE;
SELECT id,name,email,role,verified FROM users ORDER BY id DESC LIMIT 30;
