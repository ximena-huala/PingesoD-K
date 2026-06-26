Verificación de SKU y costos entre Bsale y Falabella

Antes de construir el servicio que vuelca las ventas de Falabella a la base, había que resolver dos dudas que podían tumbar el diseño: si los SKU de Falabella coinciden con los de Bsale, y si el costo de cada producto está disponible en alguna parte. Sin lo primero no se puede unir una venta con su producto; sin lo segundo no hay margen posible. Esto es lo que encontramos con los datos reales (junio 2026).

El cruce de SKU

Comparamos el catálogo de Bsale (4.198 productos) contra los 769 que D&K tiene en Falabella: 755 (98,2%) coinciden tal cual. La conclusión práctica es que no hace falta una tabla de homologación de códigos; basta cruzar por SKU directo.

De los 14 que no calzaron a la primera, tres llevan un sufijo `-1` que Falabella agrega y se recuperan normalizándolo; uno es un código de barras cargado como SKU; los diez restantes simplemente no están en Bsale (productos que D&K vende en Falabella pero no tiene en su inventario maestro). Eso último es decisión del cliente, no arreglo de código.

Un detalle que corrige una suposición: el conector oficial Bsale–Falabella (Pivot-Falabella) no está instalado, solo el de Mercado Libre. Los SKU no coinciden por una sincronización automática, sino porque D&K usó la misma convención al cargar los productos en ambos sistemas. El resultado es igual de bueno, pero conviene tenerlo presente: nada garantiza que sigan alineados si a futuro se cargan por separado.

El costo

El costo no está en el export de productos ni en la lista de precios. Vive en el reporte de Stock, en la columna "Costo Neto Prom. Unitario", pero con una trampa: ese promedio se vuelve cero cuando el producto no tiene stock, y buena parte de lo que D&K mueve está sin stock. Con ese campo solo conseguíamos costo para 86 de 769.

La salida fue la columna "Último costo" del mismo reporte, que guarda el valor de la última compra y persiste aunque el stock esté en cero. Con ella, 690 de 769 (89,7%) quedan con costo. Los 79 que faltan se solapan con los que no están en Bsale y con productos nuevos nunca comprados.

Para descartar que confundiéramos costo con precio, los comparamos en los 690 completos: costo promedio $10.060 contra precio promedio $28.418, sin ningún caso donde el costo iguale o supere al precio. Son márgenes brutos coherentes con retail; es, efectivamente, el costo.

Ojo: esos son márgenes brutos (precio − costo). La rentabilidad real es menor, porque falta descontar la comisión de Falabella (~18%), la logística y los vouchers. Ese cálculo es trabajo del motor de rentabilidad.

Para revisar con el cliente

Dos listas que son decisión de D&K, no técnica: la decena de productos que se venden en Falabella y no existen en Bsale (sin costo mientras no se carguen), y los productos con costo cero por ser nuevos y no haberse comprado aún.

Cómo reproducirlo

Todo se hace con exportaciones del panel de Bsale, sin el token de la API. El catálogo de Falabella queda en `skus-falabella.csv`; el de Bsale se baja desde Productos; el costo sale del reporte de Stock actual exportado incluyendo los productos sin stock y quedándose con la columna "Último costo". Esos archivos viven en `DK-Backend/datos/` (ver el instructivo de repoblado).
