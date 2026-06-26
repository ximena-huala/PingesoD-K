package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.Rentabilidad;
import cl.dk.rentabilidad.repository.RentabilidadRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio que genera el reporte Excel de rentabilidad.
 *
 * Genera un .xlsx con 4 pestañas:
 *   - Por Producto:   una fila por producto, con sus totales agregados.
 *   - Por Canal:      resumen agrupado por canal de venta.
 *   - Por Categoría:  resumen agrupado por categoría.
 *   - Detalle de Ventas: una fila por venta, con todos sus costos desglosados.
 *
 * En los resúmenes el margen es ponderado (margen total / ingreso total), que es
 * el margen real del grupo; en el detalle, el margen es el de cada venta.
 */
@Service
@RequiredArgsConstructor
public class ReporteExcelService {

    private final RentabilidadRepository rentabilidadRepository;

    @Transactional(readOnly = true)
    public byte[] generar(LocalDate desde, LocalDate hasta,
                          UUID canalId, String categoria) throws Exception {

        List<Rentabilidad> datos = rentabilidadRepository
                .filtrar(desde, hasta, canalId, categoria);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle estiloHeader = crearEstiloHeader(workbook);

            crearPestanaPorProducto(workbook, datos, estiloHeader);
            crearResumen(workbook, "Por Canal", "Canal", datos, estiloHeader,
                    r -> r.getVenta().getCanal().getNombre());
            crearResumen(workbook, "Por Categoría", "Categoría", datos, estiloHeader,
                    r -> r.getVenta().getProducto().getCategoria() != null
                            ? r.getVenta().getProducto().getCategoria()
                            : "Sin categoría");
            crearPestanaDetalle(workbook, datos, estiloHeader);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** Pestaña agregada: una fila por producto con sus totales. */
    private void crearPestanaPorProducto(XSSFWorkbook workbook,
                                         List<Rentabilidad> datos,
                                         CellStyle estiloHeader) {
        Sheet sheet = workbook.createSheet("Por Producto");
        String[] headers = {
                "SKU", "Producto", "Categoría", "Unidades Vendidas",
                "Ingreso Neto Total", "Costo Total", "Margen Bruto Total", "Margen %"
        };
        crearFila(sheet, 0, headers, estiloHeader);

        datos.stream()
                .collect(Collectors.groupingBy(r -> r.getVenta().getProducto().getSku()))
                .forEach((sku, lista) -> {
                    Row fila = sheet.createRow(sheet.getLastRowNum() + 1);
                    var prod = lista.get(0).getVenta().getProducto();
                    int unidades = lista.stream()
                            .mapToInt(r -> r.getVenta().getCantidad() != null ? r.getVenta().getCantidad() : 1)
                            .sum();
                    BigDecimal ingreso = sumar(lista, Rentabilidad::getIngresoNeto);
                    BigDecimal costo = sumar(lista, Rentabilidad::getCostoTotal);
                    BigDecimal margen = sumar(lista, Rentabilidad::getMargenBruto);

                    fila.createCell(0).setCellValue(sku);
                    fila.createCell(1).setCellValue(prod.getNombre());
                    fila.createCell(2).setCellValue(prod.getCategoria() != null ? prod.getCategoria() : "Sin categoría");
                    fila.createCell(3).setCellValue(unidades);
                    fila.createCell(4).setCellValue(ingreso.doubleValue());
                    fila.createCell(5).setCellValue(costo.doubleValue());
                    fila.createCell(6).setCellValue(margen.doubleValue());
                    fila.createCell(7).setCellValue(margenPonderado(margen, ingreso).doubleValue());
                });

        autoAjustar(sheet, headers.length);
    }

    /** Pestañas de resumen (Por Canal / Por Categoría): mismo formato, distinta agrupación. */
    private void crearResumen(XSSFWorkbook workbook, String nombrePestana, String tituloGrupo,
                              List<Rentabilidad> datos, CellStyle estiloHeader,
                              Function<Rentabilidad, String> agrupador) {
        Sheet sheet = workbook.createSheet(nombrePestana);
        String[] headers = {
                tituloGrupo, "Total Ventas", "Ingreso Neto Total",
                "Costo Total", "Margen Bruto Total", "Margen %"
        };
        crearFila(sheet, 0, headers, estiloHeader);

        datos.stream()
                .collect(Collectors.groupingBy(agrupador))
                .forEach((grupo, lista) -> {
                    Row fila = sheet.createRow(sheet.getLastRowNum() + 1);
                    BigDecimal ingreso = sumar(lista, Rentabilidad::getIngresoNeto);
                    BigDecimal costo = sumar(lista, Rentabilidad::getCostoTotal);
                    BigDecimal margen = sumar(lista, Rentabilidad::getMargenBruto);

                    fila.createCell(0).setCellValue(grupo);
                    fila.createCell(1).setCellValue(lista.size());
                    fila.createCell(2).setCellValue(ingreso.doubleValue());
                    fila.createCell(3).setCellValue(costo.doubleValue());
                    fila.createCell(4).setCellValue(margen.doubleValue());
                    fila.createCell(5).setCellValue(margenPonderado(margen, ingreso).doubleValue());
                });

        autoAjustar(sheet, headers.length);
    }

    /** Pestaña de detalle: una fila por venta, con todos sus costos desglosados. */
    private void crearPestanaDetalle(XSSFWorkbook workbook,
                                     List<Rentabilidad> datos,
                                     CellStyle estiloHeader) {
        Sheet sheet = workbook.createSheet("Detalle de Ventas");
        String[] headers = {
                "Fecha", "SKU", "Producto", "Categoría", "Canal",
                "Precio Venta", "Descuento Campaña", "Ingreso Neto",
                "Costo Producto", "Costo Operacional", "Costo Total",
                "Margen Bruto", "Margen %"
        };
        crearFila(sheet, 0, headers, estiloHeader);

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

        autoAjustar(sheet, headers.length);
    }

    private BigDecimal sumar(List<Rentabilidad> lista, Function<Rentabilidad, BigDecimal> campo) {
        return lista.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Margen real del grupo: margen total / ingreso total × 100 (evita dividir por cero). */
    private BigDecimal margenPonderado(BigDecimal margen, BigDecimal ingreso) {
        return ingreso.signum() == 0
                ? BigDecimal.ZERO
                : margen.divide(ingreso, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private void autoAjustar(Sheet sheet, int columnas) {
        for (int i = 0; i < columnas; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void crearFila(Sheet sheet, int numFila, String[] valores, CellStyle estilo) {
        Row fila = sheet.createRow(numFila);
        for (int i = 0; i < valores.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(valores[i]);
            celda.setCellStyle(estilo);
        }
    }

    private CellStyle crearEstiloHeader(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }
}
