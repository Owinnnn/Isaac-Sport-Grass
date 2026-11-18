-- ACTUALIZACION REPETIBLE: conserva cuentas, productos y reservas de la base anterior.
USE isaac_sport_grass;
DELIMITER $$
DROP PROCEDURE IF EXISTS isg_upgrade_payments $$
CREATE PROCEDURE isg_upgrade_payments()
BEGIN
 DECLARE n INT DEFAULT 0;
 ALTER TABLE reservations MODIFY status ENUM('CONFIRMADA','CANCELADA','PENDIENTE_PAGO','EN_REVISION','RECHAZADA','EXPIRADA') NOT NULL DEFAULT 'PENDIENTE_PAGO';
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='amount_total';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN amount_total DECIMAL(10,2) NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='deposit_amount';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN deposit_amount DECIMAL(10,2) NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='hold_expires';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN hold_expires DATETIME NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='payment_reference';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN payment_reference VARCHAR(100) NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='proof_filename';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN proof_filename VARCHAR(90) NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='reviewed_at';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN reviewed_at DATETIME NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='reviewed_by';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN reviewed_by BIGINT NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='rejection_reason';
 IF n=0 THEN ALTER TABLE reservations ADD COLUMN rejection_reason VARCHAR(250) NULL; END IF;
 SELECT COUNT(*) INTO n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND COLUMN_NAME='active_slot';
 IF n>0 THEN
  SELECT COUNT(*) INTO n FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND INDEX_NAME='ux_reservations_active_slot';
  IF n>0 THEN ALTER TABLE reservations DROP INDEX ux_reservations_active_slot; END IF;
  SELECT COUNT(*) INTO n FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND INDEX_NAME='active_slot';
  IF n>0 THEN ALTER TABLE reservations DROP INDEX active_slot; END IF;
  ALTER TABLE reservations DROP COLUMN active_slot;
 END IF;
 ALTER TABLE reservations ADD COLUMN active_slot VARCHAR(120) GENERATED ALWAYS AS (CASE WHEN status IN ('CONFIRMADA','PENDIENTE_PAGO','EN_REVISION') THEN CONCAT(court_id,'/',booking_date,'/',booking_time) ELSE NULL END) STORED;
 CREATE UNIQUE INDEX ux_reservations_active_slot ON reservations(active_slot);
 SELECT COUNT(*) INTO n FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservations' AND INDEX_NAME='ix_reservations_hold';
 IF n=0 THEN CREATE INDEX ix_reservations_hold ON reservations(status,hold_expires); END IF;
 UPDATE reservations r JOIN courts c ON c.id=r.court_id SET r.amount_total=c.price,r.deposit_amount=ROUND(c.price*0.5,2) WHERE r.amount_total IS NULL;
END $$
DELIMITER ;
CALL isg_upgrade_payments();
DROP PROCEDURE isg_upgrade_payments;
CREATE TABLE IF NOT EXISTS booking_audit (id BIGINT AUTO_INCREMENT PRIMARY KEY,reservation_id BIGINT NOT NULL,actor_user_id BIGINT NULL,action VARCHAR(40) NOT NULL,notes VARCHAR(300),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(reservation_id) REFERENCES reservations(id));
CREATE TABLE IF NOT EXISTS payment_settings (config_key VARCHAR(50) PRIMARY KEY, config_value VARCHAR(500) NOT NULL);
SELECT 'Actualización de pagos completada' AS resultado;
