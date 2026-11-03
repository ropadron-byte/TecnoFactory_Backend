CREATE TABLE contacto (
    id_contacto BIGINT AUTO_INCREMENT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(250) NOT NULL,
    comentario VARCHAR(500) NOT NULL,
    CONSTRAINT contacto_PK PRIMARY KEY (id_contacto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;