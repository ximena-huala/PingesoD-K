-- Campos adicionales para carga manual desde exportaciones Bsale
ALTER TABLE producto ADD COLUMN IF NOT EXISTS marca VARCHAR(100);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS tipo_producto VARCHAR(100);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS stock NUMERIC(12, 2) NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_producto_marca ON producto(marca);
CREATE INDEX IF NOT EXISTS idx_producto_tipo ON producto(tipo_producto);
