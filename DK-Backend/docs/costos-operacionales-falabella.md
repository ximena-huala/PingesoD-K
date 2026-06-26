Costos operacionales de Falabella: por qué el estado de cuenta es la fuente

La primera versión de la rentabilidad descontaba solo la comisión, y se quedaba corta: Falabella le cobra al vendedor bastante más. Esto explica qué cobra, de dónde lo sacamos y cómo quedó el modelo (junio 2026).

Lo que cobra, además de la comisión

El estado de cuenta (Pagos → Estados de Cuenta → Órdenes y Transacciones) trae una fila por cargo. Los grandes son dos: la comisión (~18% de la venta) y el cofinanciamiento logístico (~12-15%). Más atrás aparecen publicidad y devoluciones. Entre comisión y logística, Falabella se lleva alrededor de un tercio de cada venta, antes incluso del costo del producto.

El hallazgo clave fue la logística: es casi tan grande como la comisión y al principio la ignorábamos. En la API el costo de envío del vendedor venía siempre en cero, lo que nos hizo creer que no había costo logístico; sí lo hay, pero se cobra aparte y solo aparece en el estado de cuenta.

Por qué no se aproxima con una tasa

Con la comisión funciona una tasa por categoría, porque es un porcentaje parejo del precio. Con la logística no: los montos reales van de ~1.000 a ~22.000 pesos por unidad, y como porcentaje de la venta saltan entre 5% y 65%, porque es un costo por paquete (peso y destino), no proporcional al precio. A un producto barato le pesa mucho; a uno caro, casi nada. Cualquier tasa única daría márgenes equivocados, así que la logística se toma del valor real del estado de cuenta.

Cómo quedó el modelo

El estado de cuenta es la fuente de los costos por venta. Los costos reales quedan en la tabla `costo_venta`, una fila por tipo (comisión, logística) con su monto.

El cruce con nuestras ventas se hace por unidad, con el "Id Artículo" del reporte, que es el mismo identificador (`OrderItemId`) con que guardamos cada venta en `referencia_externa`. Esto importa: cruzar por orden + SKU infla los costos en las órdenes con varias unidades del mismo producto, porque le carga el total de la orden a cada unidad. Por unidad, cada venta recibe exactamente su comisión y su logística.

El motor usa una regla simple: si la venta ya tiene costos del estado de cuenta, usa esos —exactos—; si todavía no se liquida, estima la comisión por categoría (sin logística). Cuando llega el siguiente estado de cuenta, esa venta pasa a su costo real. Es híbrido: preciso para lo liquidado, estimado para lo reciente, y se corrige solo.

El efecto en el margen

Vale tenerlo presente para no prometer de más. Con una comisión plana de 6,97% el margen del canal parecía cercano al 49%; con la comisión real por categoría bajó al rango de 30%, y sumando la logística real quedó en torno al 26%. Ese es el margen de contribución de verdad —todavía sin los gastos generales de la empresa (publicidad, sueldos, bodega)—.

Pendientes

La publicidad es un gasto de cuenta, no por venta, así que conviene tratarla como costo general aparte. Las devoluciones todavía no se descuentan: una venta devuelta sigue contando. Ninguno cambia el grueso del margen, que ya tiene sus dos costos grandes —comisión y logística— reales.
