-- -----------------------------------------------------
-- V9: Catálogo inicial de la tienda (los 21 productos que el frontend
--     traía de fábrica en localStorage) y las categorías que faltaban.
-- Las categorías se buscan por nombre (los ids son AUTO_INCREMENT) y
-- las imágenes se reutilizan por URL (imagen_url.url es único).
-- -----------------------------------------------------

-- 1. Categorías del frontend (solo se agregan las que V3 no trae: Audio y Smartphones)
INSERT INTO categoria (nombre_categoria, descripcion_categoria)
SELECT 'Notebooks', 'Equipos portátiles para estudio, trabajo y diseño.' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre_categoria = 'Notebooks');

INSERT INTO categoria (nombre_categoria, descripcion_categoria)
SELECT 'Audio', 'Audífonos y parlantes para música, juegos y llamadas.' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre_categoria = 'Audio');

INSERT INTO categoria (nombre_categoria, descripcion_categoria)
SELECT 'Accesorios', 'Mouse, teclados y periféricos para tu escritorio.' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre_categoria = 'Accesorios');

INSERT INTO categoria (nombre_categoria, descripcion_categoria)
SELECT 'Monitores', 'Pantallas para trabajar, estudiar o jugar.' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre_categoria = 'Monitores');

INSERT INTO categoria (nombre_categoria, descripcion_categoria)
SELECT 'Almacenamiento', 'Discos duros, SSD y memorias para tus archivos.' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre_categoria = 'Almacenamiento');

INSERT INTO categoria (nombre_categoria, descripcion_categoria)
SELECT 'Smartphones', 'Teléfonos inteligentes de distintas marcas y rangos de precio.' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre_categoria = 'Smartphones');

-- 2. Productos, con su categoría y sus imágenes

-- iPhone 5s
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('iPhone 5s', 'The iPhone 5s is a classic smartphone known for its compact design and advanced features during its release. While it''s an older model, it still provides a reliable user experience.', 189990, 25, 3, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/3.webp';

-- iPhone 6
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('iPhone 6', 'The iPhone 6 is a stylish and capable smartphone with a larger display and improved performance. It introduced new features and design elements, making it a popular choice in its time.', 284990, 60, 7, 10, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-6/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-6/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-6/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-6/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-6/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-6/3.webp';

-- iPhone 13 Pro
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('iPhone 13 Pro', 'The iPhone 13 Pro is a cutting-edge smartphone with a powerful camera system, high-performance chip, and stunning display. It offers advanced features for users who demand top-notch technology.', 1044990, 56, 7, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/3.webp';

-- iPhone X
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('iPhone X', 'The iPhone X is a flagship smartphone featuring a bezel-less OLED display, facial recognition technology (Face ID), and impressive performance. It represents a milestone in iPhone design and innovation.', 854990, 37, 4, 15, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-x/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-x/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-x/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-x/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/iphone-x/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-x/3.webp';

-- Oppo A57
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Oppo A57', 'The Oppo A57 is a mid-range smartphone known for its sleek design and capable features. It offers a balance of performance and affordability, making it a popular choice.', 237490, 19, 2, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/3.webp';

-- Oppo F19 Pro Plus
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Oppo F19 Pro Plus', 'The Oppo F19 Pro Plus is a feature-rich smartphone with a focus on camera capabilities. It boasts advanced photography features and a powerful performance for a premium user experience.', 379990, 78, 9, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/3.webp';

-- Oppo K1
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Oppo K1', 'The Oppo K1 series offers a range of smartphones with various features and specifications. Known for their stylish design and reliable performance, the Oppo K1 series caters to diverse user preferences.', 284990, 55, 7, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/3.webp';

-- Realme C35
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Realme C35', 'The Realme C35 is a budget-friendly smartphone with a focus on providing essential features for everyday use. It offers a reliable performance and user-friendly experience.', 142490, 48, 6, 20, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-c35/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-c35/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-c35/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-c35/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-c35/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-c35/3.webp';

-- Realme X
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Realme X', 'The Realme X is a mid-range smartphone known for its sleek design and impressive display. It offers a good balance of performance and camera capabilities for users seeking a quality device.', 284990, 12, 2, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-x/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-x/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-x/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-x/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-x/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-x/3.webp';

-- Realme XT
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Realme XT', 'The Realme XT is a feature-rich smartphone with a focus on camera technology. It comes equipped with advanced camera sensors, delivering high-quality photos and videos for photography enthusiasts.', 332490, 80, 10, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-xt/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-xt/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-xt/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-xt/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/realme-xt/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/realme-xt/3.webp';

-- Samsung Galaxy S7
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Samsung Galaxy S7', 'The Samsung Galaxy S7 is a flagship smartphone known for its sleek design and advanced features. It features a high-resolution display, powerful camera, and robust performance.', 284990, 67, 8, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/3.webp';

-- Samsung Galaxy S8
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Samsung Galaxy S8', 'The Samsung Galaxy S8 is a premium smartphone with an Infinity Display, offering a stunning visual experience. It boasts advanced camera capabilities and cutting-edge technology.', 474990, 0, 2, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/3.webp';

-- Samsung Galaxy S10
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Samsung Galaxy S10', 'The Samsung Galaxy S10 is a flagship device featuring a dynamic AMOLED display, versatile camera system, and powerful performance. It represents innovation and excellence in smartphone technology.', 664990, 19, 2, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/3.webp';

-- Vivo S1
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Vivo S1', 'The Vivo S1 is a stylish and mid-range smartphone offering a blend of design and performance. It features a vibrant display, capable camera system, and reliable functionality.', 237490, 50, 6, 10, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/3.webp';

-- Vivo V9
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Vivo V9', 'The Vivo V9 is a smartphone known for its sleek design and emphasis on capturing high-quality selfies. It features a notch display, dual-camera setup, and a modern design.', 284990, 82, 10, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/3.webp';

-- Vivo X21
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Vivo X21', 'The Vivo X21 is a premium smartphone with a focus on cutting-edge technology. It features an in-display fingerprint sensor, a high-resolution display, and advanced camera capabilities.', 474990, 7, 2, 25, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Smartphones';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/3.webp';

-- Apple MacBook Pro 14 Inch Space Grey
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Apple MacBook Pro 14 Inch Space Grey', 'The MacBook Pro 14 Inch in Space Grey is a powerful and sleek laptop, featuring Apple''s M1 Pro chip for exceptional performance and a stunning Retina display.', 1899990, 24, 3, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Notebooks';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/3.webp';

-- Asus Zenbook Pro Dual Screen Laptop
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Asus Zenbook Pro Dual Screen Laptop', 'The Asus Zenbook Pro Dual Screen Laptop is a high-performance device with dual screens, providing productivity and versatility for creative professionals.', 1709990, 45, 5, 12, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Notebooks';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/3.webp';

-- Huawei Matebook X Pro
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Huawei Matebook X Pro', 'The Huawei Matebook X Pro is a slim and stylish laptop with a high-resolution touchscreen display, offering a premium experience for users on the go.', 1329990, 75, 9, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Notebooks';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/3.webp';

-- Lenovo Yoga 920
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('Lenovo Yoga 920', 'The Lenovo Yoga 920 is a 2-in-1 convertible laptop with a flexible hinge, allowing you to use it as a laptop or tablet, offering versatility and portability.', 1044990, 40, 5, 0, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Notebooks';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/3.webp';

-- New DELL XPS 13 9300 Laptop
INSERT INTO producto (nombre, descripcion, precio, stock, stock_critico, descuento, activo)
VALUES ('New DELL XPS 13 9300 Laptop', 'The New DELL XPS 13 9300 Laptop is a compact and powerful device, featuring a virtually borderless InfinityEdge display and high-end performance for various tasks.', 1424990, 74, 9, 15, TRUE);
SET @p = LAST_INSERT_ID();
INSERT INTO producto_categoria (producto_id_producto, categoria_id_categoria)
SELECT @p, id_categoria FROM categoria WHERE nombre_categoria = 'Notebooks';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/1.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/1.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/2.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/2.webp';
INSERT IGNORE INTO imagen_url (url) VALUES ('https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/3.webp');
INSERT INTO producto_imagen (producto_id_producto, imagen_id_url)
SELECT @p, id_url FROM imagen_url WHERE url = 'https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/3.webp';
