-- V4: comisión (y otros costos) por categoría de producto.
--
-- Falabella cobra distinta comisión según la categoría (de 11% a 20%). costo_canal
-- guardaba un costo plano por canal; ahora puede tener un costo específico por
-- categoría. categoria = NULL significa "aplica a todo el canal" (el default que se
-- usa cuando una categoría no tiene tarifa propia).

ALTER TABLE costo_canal ADD COLUMN categoria VARCHAR(100);

COMMENT ON COLUMN costo_canal.categoria IS
    'Categoría de producto a la que aplica el costo; NULL = aplica a todo el canal (default)';

CREATE INDEX idx_costo_canal_categoria
    ON costo_canal(canal_id, categoria)
    WHERE categoria IS NOT NULL;
