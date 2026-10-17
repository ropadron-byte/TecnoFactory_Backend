-- -----------------------------------------------------
-- Creación de Tablas con PK, Auto Increment e Integridad
-- -----------------------------------------------------

CREATE TABLE region (
    id_region INT NOT NULL,
    nombre_region VARCHAR(255) NOT NULL,
    CONSTRAINT region_PK PRIMARY KEY (id_region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comuna (
    id_comuna INT NOT NULL,
    nombre_comuna VARCHAR(255) NOT NULL,
    region_id_region INT NOT NULL,
    CONSTRAINT comuna_PK PRIMARY KEY (id_comuna)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tipo_usuario (
    id_tipo_usuario BIGINT AUTO_INCREMENT NOT NULL,
    nombre_tipo_usuario VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT tipo_usuario_PK PRIMARY KEY (id_tipo_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuario (
    id_usuario BIGINT AUTO_INCREMENT NOT NULL,
    run INT NOT NULL UNIQUE,
    run_dv CHAR(1) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    apellidos VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(15) NOT NULL,
    contrasenia VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_usuario_id_tipo_usuario BIGINT NOT NULL,
    comuna_id_comuna INT NOT NULL,
    CONSTRAINT usuario_PK PRIMARY KEY (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE blog (
    id_blog BIGINT AUTO_INCREMENT NOT NULL,
    usuario_id_usuario BIGINT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT blog_PK PRIMARY KEY (id_blog)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE boleta (
    id_boleta BIGINT AUTO_INCREMENT NOT NULL,
    usuario_id_usuario BIGINT NOT NULL,
    fecha DATETIME NOT NULL,
    total_boleta BIGINT NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT boleta_PK PRIMARY KEY (id_boleta),
    CONSTRAINT chk_boleta_total CHECK (total_boleta >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE detalle_boleta (
    id_detalle_boleta BIGINT AUTO_INCREMENT NOT NULL,
    boleta_id_boleta BIGINT NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    precio_unitario BIGINT NOT NULL,
    producto_id_producto BIGINT NOT NULL,
    descuento INT NOT NULL,
    cantidad INT NOT NULL,
    CONSTRAINT detalle_boleta_PK PRIMARY KEY (id_detalle_boleta),
    CONSTRAINT chk_detalle_precio CHECK (precio_unitario >= 0),
    CONSTRAINT chk_detalle_descuento CHECK (descuento BETWEEN 0 AND 100),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE carrito (
    id_carrito BIGINT AUTO_INCREMENT NOT NULL,
    usuario_id_usuario BIGINT NOT NULL UNIQUE,
    CONSTRAINT carrito_PK PRIMARY KEY (id_carrito)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE producto (
    id_producto BIGINT AUTO_INCREMENT NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    descripcion VARCHAR(500),
    precio BIGINT NOT NULL,
    stock BIGINT NOT NULL,
    stock_critico BIGINT NOT NULL,
    descuento INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT producto_PK PRIMARY KEY (id_producto),
    CONSTRAINT chk_producto_precio CHECK (precio >= 0),
    CONSTRAINT chk_producto_stock CHECK (stock >= 0),
    CONSTRAINT chk_producto_stock_critico CHECK (stock_critico >= 0),
    CONSTRAINT chk_producto_descuento CHECK (descuento BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE detalle_carrito (
    carrito_id_carrito BIGINT NOT NULL,
    producto_id_producto BIGINT NOT NULL,
    cantidad INT NOT NULL,
    CONSTRAINT detalle_carrito_PK PRIMARY KEY (carrito_id_carrito, producto_id_producto),
    CONSTRAINT chk_carrito_cantidad CHECK (cantidad >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categoria (
    id_categoria BIGINT AUTO_INCREMENT NOT NULL,
    nombre_categoria VARCHAR(255) NOT NULL UNIQUE,
    descripcion_categoria VARCHAR(255) NOT NULL,
    CONSTRAINT categoria_PK PRIMARY KEY (id_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE producto_categoria (
    producto_id_producto BIGINT NOT NULL,
    categoria_id_categoria BIGINT NOT NULL,
    CONSTRAINT producto_categoria_PK PRIMARY KEY (producto_id_producto, categoria_id_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE imagen_url (
    id_url BIGINT AUTO_INCREMENT NOT NULL,
    url VARCHAR(1000) NOT NULL,
    producto_id_producto BIGINT NULL,
    categoria_id_categoria BIGINT NULL,
    blog_id_blog BIGINT NULL,
    CONSTRAINT imagen_url_PK PRIMARY KEY (id_url),
    CONSTRAINT chk_imagen_dueno CHECK (
        (producto_id_producto IS NOT NULL AND categoria_id_categoria IS NULL AND blog_id_blog IS NULL) OR
        (producto_id_producto IS NULL AND categoria_id_categoria IS NOT NULL AND blog_id_blog IS NULL) OR
        (producto_id_producto IS NULL AND categoria_id_categoria IS NULL AND blog_id_blog IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- -----------------------------------------------------
-- Claves Foráneas (Foreign Keys)
-- -----------------------------------------------------

ALTER TABLE detalle_boleta
    ADD CONSTRAINT detalle_boleta_producto_FK FOREIGN KEY (producto_id_producto)
    REFERENCES producto (id_producto) ON DELETE RESTRICT;

ALTER TABLE comuna
    ADD CONSTRAINT comuna_region_FK FOREIGN KEY (region_id_region)
    REFERENCES region (id_region) ON DELETE RESTRICT;

ALTER TABLE usuario
    ADD CONSTRAINT usuario_comuna_FK FOREIGN KEY (comuna_id_comuna)
    REFERENCES comuna (id_comuna) ON DELETE RESTRICT;

ALTER TABLE usuario
    ADD CONSTRAINT usuario_tipo_usuario_FK FOREIGN KEY (tipo_usuario_id_tipo_usuario)
    REFERENCES tipo_usuario (id_tipo_usuario) ON DELETE RESTRICT;

ALTER TABLE blog
    ADD CONSTRAINT blog_usuario_FK FOREIGN KEY (usuario_id_usuario)
    REFERENCES usuario (id_usuario) ON DELETE RESTRICT;

ALTER TABLE boleta
    ADD CONSTRAINT boleta_usuario_FK FOREIGN KEY (usuario_id_usuario)
    REFERENCES usuario (id_usuario) ON DELETE RESTRICT;

ALTER TABLE detalle_boleta
    ADD CONSTRAINT detalle_boleta_boleta_FK FOREIGN KEY (boleta_id_boleta)
    REFERENCES boleta (id_boleta) ON DELETE RESTRICT;

ALTER TABLE carrito
    ADD CONSTRAINT carrito_usuario_FK FOREIGN KEY (usuario_id_usuario)
    REFERENCES usuario (id_usuario) ON DELETE CASCADE;

ALTER TABLE detalle_carrito
    ADD CONSTRAINT detalle_carrito_carrito_FK FOREIGN KEY (carrito_id_carrito)
    REFERENCES carrito (id_carrito) ON DELETE CASCADE;

ALTER TABLE detalle_carrito
    ADD CONSTRAINT detalle_carrito_producto_FK FOREIGN KEY (producto_id_producto)
    REFERENCES producto (id_producto) ON DELETE RESTRICT;

ALTER TABLE producto_categoria
    ADD CONSTRAINT producto_categoria_producto_FK FOREIGN KEY (producto_id_producto)
    REFERENCES producto (id_producto) ON DELETE CASCADE;

ALTER TABLE producto_categoria
    ADD CONSTRAINT producto_categoria_categoria_FK FOREIGN KEY (categoria_id_categoria)
    REFERENCES categoria (id_categoria) ON DELETE RESTRICT;

ALTER TABLE imagen_url
    ADD CONSTRAINT imagen_url_producto_FK FOREIGN KEY (producto_id_producto)
    REFERENCES producto (id_producto) ON DELETE CASCADE;

ALTER TABLE imagen_url
    ADD CONSTRAINT imagen_url_categoria_FK FOREIGN KEY (categoria_id_categoria)
    REFERENCES categoria (id_categoria) ON DELETE CASCADE;

ALTER TABLE imagen_url
    ADD CONSTRAINT imagen_url_blog_FK FOREIGN KEY (blog_id_blog)
    REFERENCES blog (id_blog) ON DELETE CASCADE;