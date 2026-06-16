-- Integración con fuentes externas (Bsale, MercadoLibre, Falabella)
-- Referencias a entidades en sistemas de origen para sincronización.

ALTER TABLE producto
    ADD COLUMN bsale_variant_id  INTEGER UNIQUE,
    ADD COLUMN bsale_product_id  INTEGER;

CREATE INDEX idx_producto_bsale_variant ON producto(bsale_variant_id);

-- Historial de sincronizaciones por fuente
CREATE TABLE integracion_sync_log (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fuente                VARCHAR(50)  NOT NULL,
    estado                VARCHAR(20)  NOT NULL,
    productos_creados     INTEGER      NOT NULL DEFAULT 0,
    productos_actualizados INTEGER     NOT NULL DEFAULT 0,
    productos_omitidos    INTEGER      NOT NULL DEFAULT 0,
    errores               INTEGER      NOT NULL DEFAULT 0,
    detalle_errores       TEXT,
    iniciado_en           TIMESTAMP    NOT NULL DEFAULT NOW(),
    finalizado_en         TIMESTAMP
);

CREATE INDEX idx_sync_log_fuente ON integracion_sync_log(fuente);
CREATE INDEX idx_sync_log_fecha  ON integracion_sync_log(iniciado_en);
