-- Login con Google: identificador de la cuenta de Google y contraseña opcional.
-- Los usuarios creados solo con Google no tienen contraseña (contrasenia NULL).
ALTER TABLE usuario
    ADD COLUMN google_id VARCHAR(255) NULL AFTER contrasenia,
    MODIFY COLUMN contrasenia VARCHAR(255) NULL,
    ADD CONSTRAINT usuario_google_id_unique UNIQUE (google_id);
