package cl.dk.rentabilidad.integration.bsale;

import cl.dk.rentabilidad.entity.IntegracionSyncLog;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleProductDto;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleSyncResultDto;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleVariantCostDto;
import cl.dk.rentabilidad.integration.bsale.dto.BsaleVariantDto;
import cl.dk.rentabilidad.repository.IntegracionSyncLogRepository;
import cl.dk.rentabilidad.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sincroniza el catálogo maestro de D&K desde Bsale.
 *
 * <p>Por cada variante activa obtiene:
 * <ul>
 *   <li>SKU → {@code variant.code}</li>
 *   <li>Costo → {@code variant.costs.averageCost}</li>
 *   <li>Nombre → producto padre + descripción de variante</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BsaleProductSyncService {

    private static final String FUENTE = "BSALE";

    private final BsaleApiClient bsaleApiClient;
    private final BsaleProperties bsaleProperties;
    private final ProductoService productoService;
    private final IntegracionSyncLogRepository syncLogRepository;

    @Transactional
    public BsaleSyncResultDto sincronizarProductos() {
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
        Map<Integer, BsaleProductDto> cacheProductos = new HashMap<>();

        try {
            int total = bsaleApiClient.contarVariantesActivas();
            int offset = 0;
            int pageSize = bsaleProperties.getPageSize();

            log.info("Iniciando sincronización Bsale: {} variantes activas estimadas", total);

            while (true) {
                var pagina = bsaleApiClient.listarVariantesActivas(offset);
                if (pagina == null || pagina.getItems() == null || pagina.getItems().isEmpty()) {
                    break;
                }

                for (BsaleVariantDto variante : pagina.getItems()) {
                    try {
                        ResultadoVariante resultado = procesarVariante(variante, cacheProductos);
                        switch (resultado) {
                            case CREADO -> creados++;
                            case ACTUALIZADO -> actualizados++;
                            case OMITIDO -> omitidos++;
                        }
                    } catch (Exception e) {
                        errores++;
                        String mensaje = "Variante " + variante.getId() + ": " + e.getMessage();
                        detalleErrores.add(mensaje);
                        log.warn("Error sincronizando variante Bsale {}: {}", variante.getId(), e.getMessage());
                    }
                }

                offset += pageSize;
                if (pagina.getNext() == null || pagina.getNext().isBlank()) {
                    break;
                }
            }

            syncLog.setEstado(errores > 0 ? "PARTIAL" : "SUCCESS");
        } catch (Exception e) {
            syncLog.setEstado("FAILED");
            errores++;
            detalleErrores.add(e.getMessage());
            log.error("Sincronización Bsale fallida", e);
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

    public IntegracionSyncLog obtenerUltimaSincronizacion() {
        return syncLogRepository.findFirstByFuenteOrderByIniciadoEnDesc(FUENTE)
                .orElse(null);
    }

    private ResultadoVariante procesarVariante(BsaleVariantDto variante,
                                               Map<Integer, BsaleProductDto> cacheProductos) {
        if (variante.getCode() == null || variante.getCode().isBlank()) {
            return ResultadoVariante.OMITIDO;
        }

        String sku = variante.getCode().trim();
        if (sku.length() > 100) {
            throw new IllegalArgumentException("SKU demasiado largo: " + sku);
        }

        BsaleVariantCostDto costoDto = bsaleApiClient.obtenerCosto(variante.getId());
        BigDecimal costo = parsearCosto(costoDto);

        int productId = parsearProductId(variante);
        BsaleProductDto productoBsale = cacheProductos.computeIfAbsent(
                productId, id -> bsaleApiClient.obtenerProducto(id));

        String nombre = construirNombre(productoBsale, variante);
        String categoria = null; // Fase 2: mapear desde product_type

        boolean esNuevo = productoService.sincronizarDesdeBsale(
                variante.getId(),
                productId,
                sku,
                nombre,
                categoria,
                costo,
                true
        );

        return esNuevo ? ResultadoVariante.CREADO : ResultadoVariante.ACTUALIZADO;
    }

    private int parsearProductId(BsaleVariantDto variante) {
        if (variante.getProduct() == null || variante.getProduct().getId() == null) {
            throw new IllegalArgumentException("Variante sin producto padre");
        }
        return Integer.parseInt(variante.getProduct().getId());
    }

    private BigDecimal parsearCosto(BsaleVariantCostDto costoDto) {
        if (costoDto == null || costoDto.getAverageCost() == null || costoDto.getAverageCost().isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(costoDto.getAverageCost()).setScale(2, RoundingMode.HALF_UP);
    }

    private String construirNombre(BsaleProductDto productoBsale, BsaleVariantDto variante) {
        String nombreProducto = productoBsale != null && productoBsale.getName() != null
                ? productoBsale.getName().trim()
                : "Producto Bsale";
        String descripcion = variante.getDescription() != null ? variante.getDescription().trim() : "";

        if (descripcion.isBlank() || descripcion.equalsIgnoreCase(nombreProducto)) {
            return truncar(nombreProducto, 255);
        }
        return truncar(nombreProducto + " - " + descripcion, 255);
    }

    private String truncar(String valor, int max) {
        return valor.length() <= max ? valor : valor.substring(0, max);
    }

    private enum ResultadoVariante {
        CREADO, ACTUALIZADO, OMITIDO
    }
}
