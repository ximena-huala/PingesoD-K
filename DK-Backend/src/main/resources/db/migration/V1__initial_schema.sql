-- ============================================================
-- D&K | Sistema de Análisis de Rentabilidad
-- Base de datos: PostgreSQL
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- USUARIOS
-- ============================================================
CREATE TABLE usuario (
                         id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         nombre        VARCHAR(100) NOT NULL,
                         email         VARCHAR(150) NOT NULL UNIQUE,
                         password_hash VARCHAR(255) NOT NULL,
                         activo        BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                         ultimo_acceso TIMESTAMP
);

INSERT INTO usuario (nombre, email, password_hash) VALUES
                                                       ('Kevin Jensen',       'kevin@dk.cl',  crypt('changeme', gen_salt('bf'))),
                                                       ('Daniel Cuevas',      'daniel@dk.cl', crypt('changeme', gen_salt('bf'))),
                                                       ('Arnely Colmenarez',  'arnely@dk.cl', crypt('changeme', gen_salt('bf')));

-- ============================================================
-- CANALES DE VENTA
-- ============================================================
CREATE TYPE tipo_canal AS ENUM (
    'MARKETPLACE',
    'TIENDA_WEB_PROPIA',
    'TIENDA_FISICA'
);

CREATE TABLE canal_venta (
                             id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             nombre  VARCHAR(100) NOT NULL UNIQUE,
                             tipo    tipo_canal   NOT NULL,
                             activo  BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO canal_venta (nombre, tipo) VALUES
                                           ('MercadoLibre',    'MARKETPLACE'),
                                           ('Falabella',       'MARKETPLACE'),
                                           ('Ripley',          'MARKETPLACE'),
                                           ('Paris',           'MARKETPLACE'),
                                           ('Walmart',         'MARKETPLACE'),
                                           ('Gaton.cl',        'TIENDA_WEB_PROPIA'),
                                           ('Eltrebolhome.cl', 'TIENDA_WEB_PROPIA'),
                                           ('Tienda Física',   'TIENDA_FISICA');

-- ============================================================
-- COSTOS OPERACIONALES POR CANAL
-- Cada canal puede tener múltiples tipos de costo.
-- Los valores se editan manualmente desde la app.
-- ============================================================
CREATE TYPE tipo_costo AS ENUM (
    'COMISION_PORCENTAJE',   -- % sobre precio venta
    'COSTO_ENVIO_FIJO',      -- monto fijo por envío
    'COSTO_ENVIO_PORCENTAJE',-- % sobre precio venta
    'COSTO_LOGISTICO',       -- fulfillment, bodegaje
    'PUBLICIDAD',            -- gasto en campaña marketplace
    'OTRO'
);

CREATE TABLE costo_canal (
                             id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             canal_id     UUID        NOT NULL REFERENCES canal_venta(id),
                             tipo_costo   tipo_costo  NOT NULL,
                             descripcion  VARCHAR(200),
                             valor        NUMERIC(10, 4) NOT NULL,   -- % o monto según tipo
                             es_porcentaje BOOLEAN    NOT NULL DEFAULT TRUE,
                             fecha_inicio DATE        NOT NULL DEFAULT CURRENT_DATE,
                             fecha_fin    DATE,                      -- NULL = vigente
                             created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_costo_canal_canal ON costo_canal(canal_id);
CREATE INDEX idx_costo_canal_vigente ON costo_canal(canal_id, fecha_fin)
    WHERE fecha_fin IS NULL;

-- ============================================================
-- PRODUCTOS
-- ============================================================
CREATE TABLE producto (
                          id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          sku        VARCHAR(100) NOT NULL UNIQUE,
                          nombre     VARCHAR(255) NOT NULL,
                          categoria  VARCHAR(100),
                          costo_base NUMERIC(12, 2) NOT NULL,   -- costo de compra/fabricación
                          activo     BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_producto_categoria ON producto(categoria);
CREATE INDEX idx_producto_sku ON producto(sku);

-- ============================================================
-- VENTAS
-- Una fila = una unidad vendida en un canal.
-- El cálculo de rentabilidad es unitario (acuerdo con cliente).
-- ============================================================
CREATE TABLE venta (
                       id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       canal_id            UUID           NOT NULL REFERENCES canal_venta(id),
                       producto_id         UUID           NOT NULL REFERENCES producto(id),
                       fecha_venta         DATE           NOT NULL,
                       precio_venta        NUMERIC(12, 2) NOT NULL,
                       cantidad            INTEGER        NOT NULL DEFAULT 1,
                       descuento_campana   NUMERIC(12, 2) NOT NULL DEFAULT 0,  -- descuento CyberDay, etc.
                       referencia_externa  VARCHAR(200),   -- order ID del marketplace
                       created_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_venta_canal       ON venta(canal_id);
CREATE INDEX idx_venta_producto    ON venta(producto_id);
CREATE INDEX idx_venta_fecha       ON venta(fecha_venta);
CREATE INDEX idx_venta_fecha_canal ON venta(fecha_venta, canal_id);

-- ============================================================
-- RENTABILIDAD (calculada, una fila por venta)
-- El motor recalcula esto al importar ventas o cambiar costos.
-- ============================================================
CREATE TABLE rentabilidad (
                              id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              venta_id            UUID           NOT NULL UNIQUE REFERENCES venta(id),
                              ingreso_neto        NUMERIC(12, 2) NOT NULL,   -- precio_venta - descuento_campana
                              costo_producto      NUMERIC(12, 2) NOT NULL,   -- costo_base del producto
                              costo_operacional   NUMERIC(12, 2) NOT NULL,   -- suma de costos del canal
                              costo_total         NUMERIC(12, 2) NOT NULL,   -- costo_producto + costo_operacional
                              margen_bruto        NUMERIC(12, 2) NOT NULL,   -- ingreso_neto - costo_total
                              margen_porcentaje   NUMERIC(8, 4)  NOT NULL,   -- margen_bruto / ingreso_neto * 100
                              calculado_en        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rentabilidad_venta ON rentabilidad(venta_id);

-- Vista útil para el dashboard y exportación Excel
CREATE VIEW v_rentabilidad_detalle AS
SELECT
    v.fecha_venta,
    cv.nombre                           AS canal,
    cv.tipo                             AS tipo_canal,
    p.sku,
    p.nombre                            AS producto,
    p.categoria,
    v.precio_venta,
    v.descuento_campana,
    v.cantidad,
    r.ingreso_neto,
    r.costo_producto,
    r.costo_operacional,
    r.costo_total,
    r.margen_bruto,
    r.margen_porcentaje,
    r.calculado_en
FROM rentabilidad r
         JOIN venta     v  ON v.id  = r.venta_id
         JOIN canal_venta cv ON cv.id = v.canal_id
         JOIN producto   p  ON p.id  = v.producto_id;

-- ============================================================
-- LOG DE AUDITORÍA
-- Registra cambios en costos, productos y configuración.
-- ============================================================
CREATE TABLE log_auditoria (
                               id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               usuario_id       UUID         REFERENCES usuario(id),
                               tabla_afectada   VARCHAR(100) NOT NULL,
                               accion           VARCHAR(20)  NOT NULL, -- INSERT / UPDATE / DELETE
                               registro_id      UUID,
                               valor_anterior   JSONB,
                               valor_nuevo      JSONB,
                               created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_log_usuario  ON log_auditoria(usuario_id);
CREATE INDEX idx_log_tabla    ON log_auditoria(tabla_afectada);
CREATE INDEX idx_log_fecha    ON log_auditoria(created_at);

-- ============================================================
-- LOG DE ACCESOS
-- ============================================================
CREATE TABLE log_acceso (
                            id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            usuario_id UUID      NOT NULL REFERENCES usuario(id),
                            accion     VARCHAR(20) NOT NULL DEFAULT 'LOGIN',  -- LOGIN / LOGOUT
                            ip         VARCHAR(45),
                            created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_acceso_usuario ON log_acceso(usuario_id);
CREATE INDEX idx_acceso_fecha   ON log_acceso(created_at);