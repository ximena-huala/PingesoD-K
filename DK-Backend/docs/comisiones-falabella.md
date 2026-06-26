Comisiones de Falabella: de dónde salen y por qué no vienen por API

La rentabilidad necesita la comisión que Falabella le cobra a D&K por cada venta, y ese dato no está donde uno esperaría. Esto es lo que encontramos (junio 2026).

Cuánto cobra

La comisión real sale del estado de cuenta del Seller Center (Pagos → Estados de Cuenta → Órdenes y Transacciones). Sobre las transacciones reales de GATON PRODUCTS va del 11% al 20% y varía por categoría, con el 20% como caso más común; el promedio efectivo, pesado por ventas, ronda el 18%.

Las líneas principales de D&K —colchas, quilts, sábanas, mantas, almohadas, maletas— pagan 20%. Más abajo: decoración navideña 17%, teteras 16%, sacos y patines 15%, loncheras 14%, audífonos 13%, limpieza 11%. Como el negocio es mayormente textil de hogar, casi todo cae en el 20%.

Importa porque el placeholder inicial (6,97%) era menos de la mitad de lo real: con él los márgenes salían demasiado optimistas.

Por qué no viene por API

La API del Seller Center (la de la firma HMAC) es operacional: catálogo y órdenes, nada financiero. Lo confirmamos probando 22 nombres de endpoints de finanzas (`GetTransactions`, `GetStatement`, `GetPayouts`, etc.): los 22 responden `E008: Invalid Action`. La documentación oficial tampoco tiene sección de finanzas ni reportes, y aclara que las ventas se entregan como estados de cuenta semanales (ciclo viernes a jueves).

Es por diseño: Falabella separa lo operacional (la API) de lo financiero (Fpay), y lo financiero solo llega como estado de cuenta. El reporte que usamos sale del panel web, no de la API.

Qué hicimos

La comisión se carga desde el estado de cuenta, no por API. El costo real de cada venta —comisión y logística por unidad— queda en la tabla `costo_venta`. Para las ventas que todavía no aparecen en ningún estado de cuenta, el motor estima la comisión con una tarifa por categoría guardada en `costo_canal`. Así lo liquidado va con su comisión exacta y lo reciente con una estimación razonable, sin un número único que aplane todo.

Como las tarifas por categoría no cambian seguido, basta refrescar el reporte cada cierto tiempo (mensual está bien) y recargar. Las ventas entran solas por la API; la comisión se actualiza del estado de cuenta.
