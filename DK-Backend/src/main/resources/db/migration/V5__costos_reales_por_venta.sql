-- V5: costos operacionales reales por venta, tomados del estado de cuenta de Falabella.
--
-- El estado de cuenta es la fuente real de los costos por orden (comisión, logística,
-- promociones, devoluciones), y varios —sobre todo la logística— no se pueden aproximar
-- con una tasa porque dependen del envío puntual. Acá los guardamos por venta.
--
-- Para poder cruzar el reporte con nuestras ventas guardamos el número de orden de
-- Falabella (el reporte usa ese número, no el OrderItemId).

ALTER TABLE venta ADD COLUMN IF NOT EXISTS numero_orden VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_venta_numero_orden ON venta(numero_orden);

-- Bases creadas antes de Flyway tenían costo_venta con otro esquema (tipo_costo/valor).
-- Si la tabla existe pero no tiene la columna "tipo", la reemplazamos (estaba vacía en dev).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'costo_venta'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'costo_venta' AND column_name = 'tipo'
    ) THEN
        DROP TABLE costo_venta;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS costo_venta (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id   UUID         NOT NULL REFERENCES venta(id) ON DELETE CASCADE,
    tipo       VARCHAR(40)  NOT NULL,   -- COMISION, LOGISTICO, PROMOCION, DEVOLUCION, OTRO
    monto      NUMERIC(12, 2) NOT NULL, -- magnitud del costo (positivo)
    fuente     VARCHAR(40)  NOT NULL DEFAULT 'ESTADO_CUENTA',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_costo_venta UNIQUE (venta_id, tipo)
);

CREATE INDEX IF NOT EXISTS idx_costo_venta_venta ON costo_venta(venta_id);
