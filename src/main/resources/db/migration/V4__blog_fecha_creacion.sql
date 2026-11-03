-- -----------------------------------------------------
-- V4: fecha de creación del blog
-- -----------------------------------------------------

ALTER TABLE blog
    ADD COLUMN fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
