-- -----------------------------------------------------
-- V5: una imagen puede pertenecer a varios dueños.
--
-- Antes: imagen_url tenía exactamente un dueño (producto, categoría o blog)
--         y el CHECK chk_imagen_dueno lo obligaba.
-- Ahora: la imagen vive una sola vez en imagen_url y las relaciones van en
--        tablas puente. El blog además tiene una portada (imagen_principal_id)
--        y una galería (blog_imagen).
-- -----------------------------------------------------

-- 1. Portada del blog (una sola imagen)
ALTER TABLE blog
    ADD COLUMN imagen_principal_id BIGINT NULL;

ALTER TABLE blog
    ADD CONSTRAINT blog_imagen_principal_FK
    FOREIGN KEY (imagen_principal_id) REFERENCES imagen_url (id_url) ON DELETE SET NULL;

-- 2. Galería del blog y relaciones de producto/categoría
CREATE TABLE blog_imagen (
    blog_id_blog BIGINT NOT NULL,
    imagen_id_url BIGINT NOT NULL,
    CONSTRAINT blog_imagen_PK PRIMARY KEY (blog_id_blog, imagen_id_url),
    CONSTRAINT blog_imagen_blog_FK FOREIGN KEY (blog_id_blog)
        REFERENCES blog (id_blog) ON DELETE CASCADE,
    CONSTRAINT blog_imagen_imagen_FK FOREIGN KEY (imagen_id_url)
        REFERENCES imagen_url (id_url) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE producto_imagen (
    producto_id_producto BIGINT NOT NULL,
    imagen_id_url BIGINT NOT NULL,
    CONSTRAINT producto_imagen_PK PRIMARY KEY (producto_id_producto, imagen_id_url),
    CONSTRAINT producto_imagen_producto_FK FOREIGN KEY (producto_id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE,
    CONSTRAINT producto_imagen_imagen_FK FOREIGN KEY (imagen_id_url)
        REFERENCES imagen_url (id_url) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categoria_imagen (
    categoria_id_categoria BIGINT NOT NULL,
    imagen_id_url BIGINT NOT NULL,
    CONSTRAINT categoria_imagen_PK PRIMARY KEY (categoria_id_categoria, imagen_id_url),
    CONSTRAINT categoria_imagen_categoria_FK FOREIGN KEY (categoria_id_categoria)
        REFERENCES categoria (id_categoria) ON DELETE CASCADE,
    CONSTRAINT categoria_imagen_imagen_FK FOREIGN KEY (imagen_id_url)
        REFERENCES imagen_url (id_url) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Migrar los dueños existentes antes de borrar las columnas viejas
--    Si un blog tenía varias imágenes, se conserva la de menor id como portada.
UPDATE blog b
JOIN (
    SELECT blog_id_blog, MIN(id_url) AS id_url
    FROM imagen_url
    WHERE blog_id_blog IS NOT NULL
    GROUP BY blog_id_blog
) x ON x.blog_id_blog = b.id_blog
SET b.imagen_principal_id = x.id_url;

INSERT INTO blog_imagen (blog_id_blog, imagen_id_url)
SELECT blog_id_blog, id_url
FROM imagen_url
WHERE blog_id_blog IS NOT NULL;

INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT producto_id_producto, id_url
FROM imagen_url
WHERE producto_id_producto IS NOT NULL;

INSERT INTO categoria_imagen (categoria_id_categoria, imagen_id_url)
SELECT categoria_id_categoria, id_url
FROM imagen_url
WHERE categoria_id_categoria IS NOT NULL;

-- 4. Quitar el modelo de un solo dueño
ALTER TABLE imagen_url DROP CHECK chk_imagen_dueno;

ALTER TABLE imagen_url DROP FOREIGN KEY imagen_url_blog_FK;
ALTER TABLE imagen_url DROP FOREIGN KEY imagen_url_producto_FK;
ALTER TABLE imagen_url DROP FOREIGN KEY imagen_url_categoria_FK;

ALTER TABLE imagen_url DROP COLUMN blog_id_blog;
ALTER TABLE imagen_url DROP COLUMN producto_id_producto;
ALTER TABLE imagen_url DROP COLUMN categoria_id_categoria;

-- 5. Una fila por URL: la misma imagen se reutiliza en varios dueños.
--    El índice es por prefijo porque utf8mb4 usa 4 bytes por carácter y
--    un índice completo de 1000 caracteres supera el máximo de 3072 bytes.
ALTER TABLE imagen_url
    ADD CONSTRAINT uk_imagen_url_url UNIQUE (url(768));
