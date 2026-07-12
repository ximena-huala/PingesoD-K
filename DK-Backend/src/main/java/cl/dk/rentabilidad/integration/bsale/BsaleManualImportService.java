package cl.dk.rentabilidad.integration.bsale;

import cl.dk.rentabilidad.entity.IntegracionSyncLog;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleSyncResultDto;
import cl.dk.rentabilidad.repository.IntegracionSyncLogRepository;
import cl.dk.rentabilidad.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Importa el catálogo maestro desde archivos exportados manualmente en Bsale.
 *
 * <p>Flujo esperado:
 * <ol>
 *   <li>Exportar <strong>Productos y servicios</strong> → SKU, estado, marca, tipo</li>
 *   <li>Exportar <strong>Stock actual</strong> → stock y costo promedio</li>
 * </ol>
 *
 * <p>No requiere token de API. Las listas de precio por marketplace quedan fuera de alcance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BsaleManualImportService {

    private static final String FUENTE = "BSALE_MANUAL";

    private final BsaleSpreadsheetReader spreadsheetReader;
    private final ProductoService productoService;
    private final IntegracionSyncLogRepository syncLogRepository;

    @Transactional
    public BsaleSyncResultDto importar(MultipartFile archivoProductos, MultipartFile archivoStock) {
        if ((archivoProductos == null || archivoProductos.isEmpty())
                && (archivoStock == null || archivoStock.isEmpty())) {
            throw new BsaleImportException(
                    "Debes enviar al menos un archivo: 'productos' (Productos y servicios) y/o 'stock' (Stock actual)");
        }

        IntegracionSyncLog syncLog = IntegracionSyncLog.builder()
                .fuente(FUENTE)
                .estado("RUNNING")
                .build();
        syncLogRepository.save(syncLog);

        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        int errores = 0;
        List<String> detalleErrores = new ArrayList<>();

        try {
            if (archivoProductos != null && !archivoProductos.isEmpty()) {
                var filas = spreadsheetReader.leer(archivoProductos, BsaleSpreadsheetReader.TipoExportacion.PRODUCTOS);
                for (int i = 0; i < filas.size(); i++) {
                    try {
                        boolean esNuevo = procesarFilaProductos(filas.get(i));
                        if (esNuevo) {
                            creados++;
                        } else {
                            actualizados++;
                        }
                    } catch (Exception e) {
                        errores++;
                        detalleErrores.add("Productos fila " + (i + 2) + ": " + e.getMessage());
                    }
                }
            }

            if (archivoStock != null && !archivoStock.isEmpty()) {
                var filas = spreadsheetReader.leer(archivoStock, BsaleSpreadsheetReader.TipoExportacion.STOCK);
                for (int i = 0; i < filas.size(); i++) {
                    try {
                        ResultadoStock resultado = procesarFilaStock(filas.get(i));
                        switch (resultado) {
                            case CREADO -> creados++;
                            case ACTUALIZADO -> actualizados++;
                            case OMITIDO -> omitidos++;
                        }
                    } catch (Exception e) {
                        errores++;
                        detalleErrores.add("Stock fila " + (i + 2) + ": " + e.getMessage());
                    }
                }
            }

            syncLog.setEstado(errores > 0 ? "PARTIAL" : "SUCCESS");
        } catch (Exception e) {
            syncLog.setEstado("FAILED");
            errores++;
            detalleErrores.add(e.getMessage());
            log.error("Importación manual Bsale fallida", e);
        }

        syncLog.setProductosCreados(creados);
        syncLog.setProductosActualizados(actualizados);
        syncLog.setProductosOmitidos(omitidos);
        syncLog.setErrores(errores);
        if (!detalleErrores.isEmpty()) {
            syncLog.setDetalleErrores(String.join("\n", detalleErrores.stream().limit(50).toList()));
        }
        syncLog.setFinalizadoEn(LocalDateTime.now());
        syncLogRepository.save(syncLog);

        return BsaleSyncResultDto.builder()
                .productosCreados(creados)
                .productosActualizados(actualizados)
                .productosOmitidos(omitidos)
                .errores(errores)
                .totalProcesados(creados + actualizados + omitidos)
                .sincronizadoEn(syncLog.getFinalizadoEn())
                .detalleErrores(detalleErrores.stream().limit(20).toList())
                .build();
    }

    public IntegracionSyncLog obtenerUltimaImportacion() {
        return syncLogRepository.findFirstByFuenteOrderByIniciadoEnDesc(FUENTE)
                .orElse(null);
    }

    private boolean procesarFilaProductos(BsaleSpreadsheetReader.FilaImportacion fila) {
        String sku = resolverSku(fila);
        String nombre = fila.get("nombre");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("Falta nombre de producto");
        }

        return productoService.importarDesdeBsale(
                sku,
                nombre,
                fila.get("marca"),
                fila.get("tipo"),
                fila.get("categoria"),
                parsearActivo(fila.get("estado")),
                null,
                null
        );
    }

    private ResultadoStock procesarFilaStock(BsaleSpreadsheetReader.FilaImportacion fila) {
        String sku = resolverSku(fila);
        String nombre = fila.get("nombre");
        BigDecimal stock = parsearNumero(fila.get("stock"));
        BigDecimal costo = parsearNumero(fila.get("costo"));

        if (stock == null && costo == null) {
            return ResultadoStock.OMITIDO;
        }

        boolean existia = productoService.existePorSku(sku);
        boolean esNuevo = productoService.importarDesdeBsale(
                sku,
                nombre.isBlank() ? sku : nombre,
                null,
                null,
                null,
                true,
                stock,
                costo
        );

        if (esNuevo) {
            return ResultadoStock.CREADO;
        }
        return existia ? ResultadoStock.ACTUALIZADO : ResultadoStock.CREADO;
    }

    private String resolverSku(BsaleSpreadsheetReader.FilaImportacion fila) {
        String sku = fila.get("sku");
        if (!sku.isBlank()) {
            return truncar(sku, 100);
        }
        String nombre = fila.get("nombre");
        if (!nombre.isBlank()) {
            return truncar(nombre, 100);
        }
        throw new IllegalArgumentException("Falta SKU o nombre para identificar el producto");
    }

    static boolean parsearActivo(String estado) {
        if (estado == null || estado.isBlank()) {
            return true;
        }
        String normalizado = BsaleSpreadsheetReader.normalizar(estado);
        return !normalizado.contains("inactivo")
                && !normalizado.contains("desactiv")
                && !normalizado.equals("no");
    }

    static BigDecimal parsearNumero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor
                .replace("$", "")
                .replace("CLP", "")
                .replace(" ", "")
                .trim();

        if (limpio.isBlank() || limpio.equals("-")) {
            return null;
        }

        // Formato chileno: 24.182.936 (miles) o 2.672,0 (decimal)
        if (limpio.matches(".*,\\d{1,2}$")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else if (limpio.contains(".") && !limpio.contains(",")) {
            limpio = limpio.replace(".", "");
        } else {
            limpio = limpio.replace(",", "");
        }

        return new BigDecimal(limpio).setScale(2, RoundingMode.HALF_UP);
    }

    private String truncar(String valor, int max) {
        return valor.length() <= max ? valor : valor.substring(0, max);
    }

    private enum ResultadoStock {
        CREADO, ACTUALIZADO, OMITIDO
    }
}
