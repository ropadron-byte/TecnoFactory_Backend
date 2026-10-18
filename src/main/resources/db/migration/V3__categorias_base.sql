-- -----------------------------------------------------
-- V3: Categorias base del catalogo (BORRADOR: ajusta nombres y descripciones
--     a lo que venda TecnoFactory ANTES de aplicarlo).
-- Los ids son AUTO_INCREMENT: en el codigo busca por nombre, no por id.
-- nombre_categoria es UNICO y descripcion_categoria es obligatoria (max. 255).
-- -----------------------------------------------------

INSERT INTO categoria (nombre_categoria, descripcion_categoria) VALUES
    ('Notebooks', 'Computadores portátiles para trabajo, estudio y uso personal'),
    ('Computadores de escritorio', 'Equipos de escritorio y all-in-one armados'),
    ('Componentes', 'Procesadores, tarjetas de video, memorias RAM, placas madre y fuentes de poder'),
    ('Almacenamiento', 'Discos SSD y HDD, memorias USB y tarjetas de memoria'),
    ('Monitores', 'Pantallas para oficina, diseño y gaming'),
    ('Periféricos', 'Teclados, mouse, audífonos, cámaras web y parlantes'),
    ('Redes', 'Routers, switches, puntos de acceso y cables de red'),
    ('Accesorios', 'Cables, adaptadores, cargadores, mochilas y fundas');
