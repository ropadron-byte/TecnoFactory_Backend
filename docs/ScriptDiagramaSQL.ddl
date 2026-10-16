-- Generado por Oracle SQL Developer Data Modeler 24.3.1.351.0831
--   en:        2026-10-16 01:23:51 CLST
--   sitio:      SQL Server 2012
--   tipo:      SQL Server 2012



CREATE TABLE BLOG 
    (
     id_blog VARCHAR (255) NOT NULL , 
     USUARIO_id_usuario BIGINT NOT NULL , 
     titulo VARCHAR (255) NOT NULL , 
     contenido TEXT NOT NULL 
    )
GO

ALTER TABLE BLOG ADD CONSTRAINT BLOG_PK PRIMARY KEY CLUSTERED (id_blog)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE BOLETA 
    (
     id_boleta BIGINT NOT NULL , 
     USUARIO_id_usuario BIGINT NOT NULL , 
     cantidad_productos NUMERIC (8) , 
     fecha DATETIME NOT NULL , 
     total_boleta BIGINT NOT NULL , 
     estado BIT NOT NULL 
    )
GO

ALTER TABLE BOLETA ADD CONSTRAINT BOLETA_PK PRIMARY KEY CLUSTERED (id_boleta)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE CARRITO 
    (
     id_carrito BIGINT NOT NULL , 
     USUARIO_id_usuario BIGINT NOT NULL 
    )
GO 

    


CREATE UNIQUE NONCLUSTERED INDEX 
    CARRITO__IDX ON CARRITO 
    ( 
     USUARIO_id_usuario 
    ) 
GO

ALTER TABLE CARRITO ADD CONSTRAINT CARRITO_PK PRIMARY KEY CLUSTERED (id_carrito)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE CATEGORIA 
    (
     id_categoria BIGINT NOT NULL , 
     nombre_categoria VARCHAR (255) NOT NULL , 
     descripcion_categoria VARCHAR (255) NOT NULL 
    )
GO

ALTER TABLE CATEGORIA ADD CONSTRAINT CATEGORIA_PK PRIMARY KEY CLUSTERED (id_categoria)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE COMUNA 
    (
     id_comuna NUMERIC (4) NOT NULL , 
     nombre_comuna VARCHAR (255) NOT NULL , 
     REGION_id_region NUMERIC (3) NOT NULL 
    )
GO

ALTER TABLE COMUNA ADD CONSTRAINT COMUNA_PK PRIMARY KEY CLUSTERED (id_comuna)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE DETALLE_BOLETA 
    (
     id_detalle_boleta BIGINT , 
     nombre VARCHAR (255) NOT NULL , 
     descripcion VARCHAR (1000) NOT NULL , 
     precio_unitario BIGINT NOT NULL , 
     descuento NUMERIC (3) NOT NULL , 
     BOLETA_id_boleta BIGINT NOT NULL 
    )
GO

CREATE TABLE DETALLE_CARRITO 
    (
     PRODUCTOS_id_producto BIGINT NOT NULL , 
     CARRITO_id_carrito BIGINT NOT NULL , 
     cantidad NUMERIC (6) NOT NULL 
    )
GO

CREATE TABLE IMAGEN_URL 
    (
     id_url BIGINT NOT NULL , 
     url VARCHAR (1000) NOT NULL , 
     PRODUCTOS_id_producto BIGINT , 
     CATEGORIA_id_categoria BIGINT 
    )
GO

ALTER TABLE IMAGEN_URL ADD CONSTRAINT IMAGEN_URL_PK PRIMARY KEY CLUSTERED (id_url)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE PRODUCTO_CATEGORIA 
    (
     PRODUCTOS_id_producto BIGINT NOT NULL , 
     CATEGORIA_id_categoria BIGINT NOT NULL 
    )
GO

CREATE TABLE PRODUCTOS 
    (
     id_producto BIGINT NOT NULL , 
     nombre VARCHAR (255) NOT NULL , 
     descripcion VARCHAR (500) , 
     precio BIGINT NOT NULL , 
     stock BIGINT NOT NULL , 
     stockCritico BIGINT NOT NULL , 
     descuento NUMERIC (3) NOT NULL 
    )
GO

ALTER TABLE PRODUCTOS ADD CONSTRAINT PRODUCTOS_PK PRIMARY KEY CLUSTERED (id_producto)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE REGION 
    (
     id_region NUMERIC (3) NOT NULL , 
     nombre_region VARCHAR (255) NOT NULL 
    )
GO

ALTER TABLE REGION ADD CONSTRAINT REGION_PK PRIMARY KEY CLUSTERED (id_region)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE TIPO_USUARIO 
    (
     id_tipo_usuario BIGINT NOT NULL , 
     nombre_tipo_usuario VARCHAR (255) NOT NULL 
    )
GO

ALTER TABLE TIPO_USUARIO ADD CONSTRAINT TIPO_USUARIO_PK PRIMARY KEY CLUSTERED (id_tipo_usuario)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

CREATE TABLE USUARIO 
    (
     id_usuario BIGINT NOT NULL , 
     run NUMERIC (9) NOT NULL , 
     run_dv CHAR (1) NOT NULL , 
     nombre VARCHAR (255) NOT NULL , 
     apellidos VARCHAR (255) NOT NULL , 
     correo VARCHAR (255) NOT NULL , 
     fecha_nacimiento DATETIME NOT NULL , 
     direccion VARCHAR (255) NOT NULL , 
     TIPO_USUARIO_id_tipo_usuario BIGINT NOT NULL , 
     COMUNA_id_comuna NUMERIC (4) NOT NULL , 
     telefono NUMERIC (11) NOT NULL , 
     contrasenia VARCHAR (255) NOT NULL 
    )
GO

ALTER TABLE USUARIO ADD CONSTRAINT USUARIO_PK PRIMARY KEY CLUSTERED (id_usuario)
     WITH (
     ALLOW_PAGE_LOCKS = ON , 
     ALLOW_ROW_LOCKS = ON )
GO

ALTER TABLE BLOG 
    ADD CONSTRAINT BLOG_USUARIO_FK FOREIGN KEY 
    ( 
     USUARIO_id_usuario
    ) 
    REFERENCES USUARIO 
    ( 
     id_usuario 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE BOLETA 
    ADD CONSTRAINT BOLETA_USUARIO_FK FOREIGN KEY 
    ( 
     USUARIO_id_usuario
    ) 
    REFERENCES USUARIO 
    ( 
     id_usuario 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE CARRITO 
    ADD CONSTRAINT CARRITO_USUARIO_FK FOREIGN KEY 
    ( 
     USUARIO_id_usuario
    ) 
    REFERENCES USUARIO 
    ( 
     id_usuario 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE COMUNA 
    ADD CONSTRAINT COMUNA_REGION_FK FOREIGN KEY 
    ( 
     REGION_id_region
    ) 
    REFERENCES REGION 
    ( 
     id_region 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE DETALLE_BOLETA 
    ADD CONSTRAINT DETALLE_BOLETA_BOLETA_FK FOREIGN KEY 
    ( 
     BOLETA_id_boleta
    ) 
    REFERENCES BOLETA 
    ( 
     id_boleta 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE DETALLE_CARRITO 
    ADD CONSTRAINT DETALLE_CARRITO_CARRITO_FK FOREIGN KEY 
    ( 
     CARRITO_id_carrito
    ) 
    REFERENCES CARRITO 
    ( 
     id_carrito 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE DETALLE_CARRITO 
    ADD CONSTRAINT DETALLE_CARRITO_PRODUCTOS_FK FOREIGN KEY 
    ( 
     PRODUCTOS_id_producto
    ) 
    REFERENCES PRODUCTOS 
    ( 
     id_producto 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE IMAGEN_URL 
    ADD CONSTRAINT IMAGEN_URL_CATEGORIA_FK FOREIGN KEY 
    ( 
     CATEGORIA_id_categoria
    ) 
    REFERENCES CATEGORIA 
    ( 
     id_categoria 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE IMAGEN_URL 
    ADD CONSTRAINT IMAGEN_URL_PRODUCTOS_FK FOREIGN KEY 
    ( 
     PRODUCTOS_id_producto
    ) 
    REFERENCES PRODUCTOS 
    ( 
     id_producto 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE PRODUCTO_CATEGORIA 
    ADD CONSTRAINT PRODUCTO_CATEGORIA_CATEGORIA_FK FOREIGN KEY 
    ( 
     CATEGORIA_id_categoria
    ) 
    REFERENCES CATEGORIA 
    ( 
     id_categoria 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE PRODUCTO_CATEGORIA 
    ADD CONSTRAINT PRODUCTO_CATEGORIA_PRODUCTOS_FK FOREIGN KEY 
    ( 
     PRODUCTOS_id_producto
    ) 
    REFERENCES PRODUCTOS 
    ( 
     id_producto 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE USUARIO 
    ADD CONSTRAINT USUARIO_COMUNA_FK FOREIGN KEY 
    ( 
     COMUNA_id_comuna
    ) 
    REFERENCES COMUNA 
    ( 
     id_comuna 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO

ALTER TABLE USUARIO 
    ADD CONSTRAINT USUARIO_TIPO_USUARIO_FK FOREIGN KEY 
    ( 
     TIPO_USUARIO_id_tipo_usuario
    ) 
    REFERENCES TIPO_USUARIO 
    ( 
     id_tipo_usuario 
    ) 
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION 
GO



-- Informe de Resumen de Oracle SQL Developer Data Modeler: 
-- 
-- CREATE TABLE                            13
-- CREATE INDEX                             1
-- ALTER TABLE                             23
-- CREATE VIEW                              0
-- ALTER VIEW                               0
-- CREATE PACKAGE                           0
-- CREATE PACKAGE BODY                      0
-- CREATE PROCEDURE                         0
-- CREATE FUNCTION                          0
-- CREATE TRIGGER                           0
-- ALTER TRIGGER                            0
-- CREATE DATABASE                          0
-- CREATE DEFAULT                           0
-- CREATE INDEX ON VIEW                     0
-- CREATE ROLLBACK SEGMENT                  0
-- CREATE ROLE                              0
-- CREATE RULE                              0
-- CREATE SCHEMA                            0
-- CREATE SEQUENCE                          0
-- CREATE PARTITION FUNCTION                0
-- CREATE PARTITION SCHEME                  0
-- 
-- DROP DATABASE                            0
-- 
-- ERRORS                                   0
-- WARNINGS                                 0
