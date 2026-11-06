-- Unicidad real para los nombres de referencia que las entidades ya declaraban unique = true.
-- Antes de aplicar esta migración en una base existente, comprobar que no haya duplicados:
--   SELECT nombre_region, COUNT(*) FROM region GROUP BY nombre_region HAVING COUNT(*) > 1;
--   SELECT nombre_comuna, COUNT(*) FROM comuna GROUP BY nombre_comuna HAVING COUNT(*) > 1;
-- Los datos iniciales de V2 no contienen duplicados.

ALTER TABLE region
    ADD CONSTRAINT uk_region_nombre UNIQUE (nombre_region);

ALTER TABLE comuna
    ADD CONSTRAINT uk_comuna_nombre UNIQUE (nombre_comuna);
