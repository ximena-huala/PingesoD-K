CREATE TABLE IF NOT EXISTS mercadolibre_costo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(100) NOT NULL UNIQUE,
    costo_prom NUMERIC(12, 2),
    ultimo_costo NUMERIC(12, 2),
    costo_mercadolibre NUMERIC(12, 2) NOT NULL,
    fuente_archivo VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mercadolibre_costo_sku ON mercadolibre_costo(sku);
