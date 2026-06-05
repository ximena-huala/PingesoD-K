package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.Rentabilidad;
import cl.dk.rentabilidad.repository.RentabilidadRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Servicio que genera el reporte Excel de rentabilidad.
 *
 * Genera un archivo .xlsx con 3 pestañas según lo acordado con el cliente:
 *   - Pestaña 1: Detalle por producto
 *   - Pestaña 2: Resumen por canal de venta
 *   - Pestaña 3: Resumen por categoría
 *
 * Se usa Apache POI para la generación del archivo en memoria,
 * que luego se envía como stream de bytes al frontend.
 */
@Service
@RequiredArgsConstructor
public class ReporteExcelService {

    private final RentabilidadRepository rentabilidadRepository;

    /**
     * Genera el archivo Excel con los 3 tabs de rentabilidad.
     *
     * @param desde     fecha inicio del rango
     * @param hasta     fecha fin del rango
     * @param canalId   UUID del canal a filtrar (null = todos)
     * @param categoria categoría a filtrar (null = todas)
     * @return archivo Excel como arreglo de bytes listo para descargar
     */
    public byte[] generar(LocalDate desde, LocalDate hasta,
                          UUID canalId, String categoria) throws Exception {

        // Obtenemos los datos filtrados de la base de datos
        List<Rentabilidad> datos = rentabilidadRepository
                .filtrar(desde, hasta, canalId, categoria);

        // Creamos el libro Excel en memoria
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // Estilo para los headers de cada pestaña
            CellStyle estiloHeader = crearEstiloHeader(workbook);

            // Generamos las 3 pestañas
            crearPestanaProducto(workbook, datos, estiloHeader);
            crearPestanaCanal(workbook, datos, estiloHeader);
            crearPestanaCategoria(workbook, datos, estiloHeader);

            // Convertimos el libro a bytes para enviarlo como respuesta HTTP
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Pestaña 1: detalle unitario por producto.
     * Muestra cada venta con todos sus costos desglosados.
     */
    private void crearPestanaProducto(XSSFWorkbook workbook,
                                      List<Rentabilidad> datos,
                                      CellStyle estiloHeader) {
        Sheet sheet = workbook.createSheet("Por Producto");

        // Headers de la pestaña
        String[] headers = {
                "Fecha", "SKU", "Producto", "Categoría", "Canal",
                "Precio Venta", "Descuento Campaña", "Ingreso Neto",
                "Costo Producto", "Costo Operacional", "Costo Total",
                "Margen Bruto", "Margen %"
        };
        crearFila(sheet, 0, headers, estiloHeader);

        // Filas de datos
        int numFila = 1;
        for (Rentabilidad r : datos) {
            Row fila = sheet.createRow(numFila++);
            fila.createCell(0).setCellValue(r.getVenta().getFechaVenta().toString());
            fila.createCell(1).setCellValue(r.getVenta().getProducto().getSku());
            fila.createCell(2).setCellValue(r.getVenta().getProducto().getNombre());
            fila.createCell(3).setCellValue(r.getVenta().getProducto().getCategoria());
            fila.createCell(4).setCellValue(r.getVenta().getCanal().getNombre());
            fila.createCell(5).setCellValue(r.getVenta().getPrecioVenta().doubleValue());
            fila.createCell(6).setCellValue(r.getVenta().getDescuentoCampana().doubleValue());
            fila.createCell(7).setCellValue(r.getIngresoNeto().doubleValue());
            fila.createCell(8).setCellValue(r.getCostoProducto().doubleValue());
            fila.createCell(9).setCellValue(r.getCostoOperacional().doubleValue());
            fila.createCell(10).setCellValue(r.getCostoTotal().doubleValue());
            fila.createCell(11).setCellValue(r.getMargenBruto().doubleValue());
            fila.createCell(12).setCellValue(r.getMargenPorcentaje().doubleValue());
        }

        // Ajustamos el ancho de columnas automáticamente
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Pestaña 2: resumen agrupado por canal de venta.
     * Muestra totales e indicadores por canal.
     */
    private void crearPestanaCanal(XSSFWorkbook workbook,
                                   List<Rentabilidad> datos,
                                   CellStyle estiloHeader) {
        Sheet sheet = workbook.createSheet("Por Canal");

        String[] headers = {
                "Canal", "Total Ventas", "Ingreso Neto Total",
                "Costo Total", "Margen Bruto Total", "Margen % Promedio"
        };
        crearFila(sheet, 0, headers, estiloHeader);

        // Agrupamos los datos por canal
        datos.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getVenta().getCanal().getNombre()
                ))
                .forEach((canal, lista) -> {
                    int numFila = sheet.getLastRowNum() + 1;
                    Row fila = sheet.createRow(numFila);

                    // Total de ventas del canal
                    fila.createCell(0).setCellValue(canal);
                    fila.createCell(1).setCellValue(lista.size());

                    // Sumamos los valores monetarios
                    BigDecimal ingresoTotal = lista.stream()
                            .map(Rentabilidad::getIngresoNeto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal costoTotal = lista.stream()
                            .map(Rentabilidad::getCostoTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal margenTotal = lista.stream()
                            .map(Rentabilidad::getMargenBruto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Promedio del margen porcentual
                    BigDecimal margenPromedio = lista.stream()
                            .map(Rentabilidad::getMargenPorcentaje)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(lista.size()), 4,
                                    java.math.RoundingMode.HALF_UP);

                    fila.createCell(2).setCellValue(ingresoTotal.doubleValue());
                    fila.createCell(3).setCellValue(costoTotal.doubleValue());
                    fila.createCell(4).setCellValue(margenTotal.doubleValue());
                    fila.createCell(5).setCellValue(margenPromedio.doubleValue());
                });

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Pestaña 3: resumen agrupado por categoría de producto.
     */
    private void crearPestanaCategoria(XSSFWorkbook workbook,
                                       List<Rentabilidad> datos,
                                       CellStyle estiloHeader) {
        Sheet sheet = workbook.createSheet("Por Categoría");

        String[] headers = {
                "Categoría", "Total Ventas", "Ingreso Neto Total",
                "Costo Total", "Margen Bruto Total", "Margen % Promedio"
        };
        crearFila(sheet, 0, headers, estiloHeader);

        // Agrupamos los datos por categoría
        datos.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getVenta().getProducto().getCategoria() != null
                                ? r.getVenta().getProducto().getCategoria()
                                : "Sin categoría"
                ))
                .forEach((categoria, lista) -> {
                    int numFila = sheet.getLastRowNum() + 1;
                    Row fila = sheet.createRow(numFila);

                    fila.createCell(0).setCellValue(categoria);
                    fila.createCell(1).setCellValue(lista.size());

                    BigDecimal ingresoTotal = lista.stream()
                            .map(Rentabilidad::getIngresoNeto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal costoTotal = lista.stream()
                            .map(Rentabilidad::getCostoTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal margenTotal = lista.stream()
                            .map(Rentabilidad::getMargenBruto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal margenPromedio = lista.stream()
                            .map(Rentabilidad::getMargenPorcentaje)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(lista.size()), 4,
                                    java.math.RoundingMode.HALF_UP);

                    fila.createCell(2).setCellValue(ingresoTotal.doubleValue());
                    fila.createCell(3).setCellValue(costoTotal.doubleValue());
                    fila.createCell(4).setCellValue(margenTotal.doubleValue());
                    fila.createCell(5).setCellValue(margenPromedio.doubleValue());
                });

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Crea una fila de headers con estilo destacado.
     *
     * @param sheet      hoja donde se agrega la fila
     * @param numFila    número de fila (0 = primera)
     * @param valores    textos de cada celda
     * @param estilo     estilo visual a aplicar
     */
    private void crearFila(Sheet sheet, int numFila,
                           String[] valores, CellStyle estilo) {
        Row fila = sheet.createRow(numFila);
        for (int i = 0; i < valores.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(valores[i]);
            celda.setCellStyle(estilo);
        }
    }

    /**
     * Crea el estilo visual para los headers del Excel.
     * Fondo verde oscuro con texto blanco y negrita.
     */
    private CellStyle crearEstiloHeader(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();

        // Fondo verde oscuro (color USACH-ish para consistencia visual)
        estilo.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Texto en negrita y blanco
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);

        // Bordes sutiles
        estilo.setBorderBottom(BorderStyle.THIN);

        return estilo;
    }
}