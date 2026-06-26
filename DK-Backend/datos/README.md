Datos para repoblar la base

Archivos fuente y semillas para dejar la base con datos desde cero. El paso a paso está en [../docs/levantar-y-repoblar.md](../docs/levantar-y-repoblar.md).

- skus-falabella.csv — catálogo de Falabella (SKU, nombre, categoría). Fuente de la tabla `producto`.
- costos-bsale.csv — costo de cada producto en Bsale (columna `UltimoCosto`). Da el `costo_base`.
- estado-cuenta-dic-jun.csv y estado-cuenta-nov-dic.csv — estados de cuenta de Falabella; de ahí salen la comisión y la logística reales por venta.
- seed-productos.sql — generado de los dos primeros. Se carga con `psql -f`.
- seed-costos.sql — generado de los dos estados de cuenta, cruzando por "Id Artículo". Se carga después de sincronizar las ventas.

Los `.sql` son generados, no se editan a mano. Las ventas no están acá: se traen en vivo desde la API de Falabella.

Contienen información comercial de D&K (costos y finanzas): no compartir fuera del equipo.
