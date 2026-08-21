-- Tarifas de comisión por categoría del canal Falabella (estimación de respaldo).
-- El motor usa estas tarifas SOLO cuando una venta todavía no tiene su costo real
-- del estado de cuenta (comisión + logística por unidad). Es una aproximación:
-- estima la comisión por categoría, sin logística. Valores del 11% al 20% según
-- la categoría (ver docs/comisiones-falabella.md); 18% por defecto para el resto.
--
-- canal_id de Falabella: a4789506-e1e7-4921-b28f-2c2f25f7c290
-- Regenerar el canal_id si cambia: SELECT id FROM canal_venta WHERE nombre='Falabella';

BEGIN;

-- Idempotente: limpia las comisiones estimadas previas de Falabella antes de recargar.
DELETE FROM costo_canal
 WHERE canal_id = 'a4789506-e1e7-4921-b28f-2c2f25f7c290'
   AND tipo_costo = 'COMISION_PORCENTAJE';

INSERT INTO costo_canal (id, canal_id, tipo_costo, categoria, descripcion, valor, es_porcentaje, fecha_inicio, fecha_fin, created_at)
VALUES
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', NULL,                                                              'Comisión estimada por defecto', 18, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Colchas|quilts|plumones',                                        'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Sábanas',                                                        'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Mantas no eléctricas|frazadas',                                  'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Almohadas',                                                      'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Maletas|bolsos de viaje',                                        'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Carteras de mano o colgantes|mochilas|bolsos',                   'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Mochilas, bolsos, maletas y loncheras escolares',               'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Toallas de tela|textil',                                         'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Textiles de baño',                                               'Comisión estimada', 20, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Decoración navideña',                                            'Comisión estimada', 17, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Adornos|decoración',                                             'Comisión estimada', 17, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Árboles de navidad',                                             'Comisión estimada', 17, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Teteras no eléctricas',                                          'Comisión estimada', 16, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Sacos|alfombrillas para dormir de camping',                      'Comisión estimada', 15, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Patines|Monopatines',                                            'Comisión estimada', 15, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Cajas|bolsas|loncheras portátiles para alimentos no eléctricos', 'Comisión estimada', 14, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Audífonos',                                                      'Comisión estimada', 13, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Accesorios para limpieza',                                       'Comisión estimada', 11, true, '2025-01-01', NULL, NOW()),
  (gen_random_uuid(), 'a4789506-e1e7-4921-b28f-2c2f25f7c290', 'COMISION_PORCENTAJE', 'Artículos para limpieza',                                        'Comisión estimada', 11, true, '2025-01-01', NULL, NOW());

-- MercadoLibre: comisión estimada de respaldo (se puede ajustar desde la app).
-- Toma el canal por nombre para evitar hardcodear UUID.
DELETE FROM costo_canal
 WHERE canal_id = (SELECT id FROM canal_venta WHERE nombre = 'MercadoLibre')
   AND tipo_costo = 'COMISION_PORCENTAJE';

INSERT INTO costo_canal (id, canal_id, tipo_costo, categoria, descripcion, valor, es_porcentaje, fecha_inicio, fecha_fin, created_at)
SELECT gen_random_uuid(), id, 'COMISION_PORCENTAJE', NULL,
       'Comisión estimada por defecto', 18, true, '2025-01-01', NULL, NOW()
  FROM canal_venta
 WHERE nombre = 'MercadoLibre';

COMMIT;
