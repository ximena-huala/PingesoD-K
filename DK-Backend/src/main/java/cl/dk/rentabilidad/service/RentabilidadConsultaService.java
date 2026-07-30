package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.dto.RentabilidadDetalleDto;
import cl.dk.rentabilidad.dto.RentabilidadResumenDto;
import cl.dk.rentabilidad.entity.Rentabilidad;
import cl.dk.rentabilidad.repository.CostoVentaRepository;
import cl.dk.rentabilidad.repository.RentabilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consultas de rentabilidad en JSON para el frontend.
 *
 * Es la versión "para la app" del reporte Excel: lee la tabla `rentabilidad`
 * (ya calculada por {@link RentabilidadService}) y la agrega igual que el Excel,
 * pero devolviendo DTOs. Va en transacción de solo lectura porque el mapeo toca
 * canal y producto, que son lazy (open-in-view está apagado).
 */
@Service
@RequiredArgsConstructor
public class RentabilidadConsultaService {

    private final RentabilidadRepository rentabilidadRepository;
    private final CostoVentaRepository costoVentaRepository;

    @Transactional(readOnly = true)
    public RentabilidadResumenDto resumen(LocalDate desde, LocalDate hasta,
                                          UUID canalId, String categoria) {
        List<Rentabilidad> datos = rentabilidadRepository.filtrar(desde, hasta, canalId, categoria);

        BigDecimal ingreso = sum(datos, Rentabilidad::getIngresoNeto);
        BigDecimal margen = sum(datos, Rentabilidad::getMargenBruto);
        var totales = new RentabilidadResumenDto.Totales(
                datos.size(),
                ingreso,
                sum(datos, Rentabilidad::getCostoProducto),
                sum(datos, Rentabilidad::getCostoOperacional),
                sum(datos, Rentabilidad::getCostoTotal),
                margen,
                pct(margen, ingreso));

        return new RentabilidadResumenDto(
                totales,
                agrupar(datos, RentabilidadConsultaService::categoria),
                agrupar(datos, r -> r.getVenta().getCanal().getNombre()),
                porMes(datos),
                porProducto(datos));
    }

    @Transactional(readOnly = true)
    public List<RentabilidadDetalleDto> detalle(LocalDate desde, LocalDate hasta,
                                                UUID canalId, String categoria) {
        Map<UUID, Map<String, BigDecimal>> costos = costosPorVentaYTipo();
        return rentabilidadRepository.filtrar(desde, hasta, canalId, categoria).stream()
                .map(r -> toDetalle(r, costos))
                .sorted(Comparator.comparing(RentabilidadDetalleDto::fecha).reversed())
                .toList();
    }

    /** Agrupación genérica por una clave textual (categoría, canal). */
    private List<RentabilidadResumenDto.Grupo> agrupar(List<Rentabilidad> datos,
                                                       Function<Rentabilidad, String> clave) {
        return datos.stream()
                .collect(Collectors.groupingBy(clave))
                .entrySet().stream()
                .map(e -> {
                    var lista = e.getValue();
                    BigDecimal ing = sum(lista, Rentabilidad::getIngresoNeto);
                    BigDecimal mar = sum(lista, Rentabilidad::getMargenBruto);
                    return new RentabilidadResumenDto.Grupo(
                            e.getKey(), unidades(lista), ing,
                            sum(lista, Rentabilidad::getCostoTotal), mar, pct(mar, ing));
                })
                .sorted(Comparator.comparing(RentabilidadResumenDto.Grupo::margen).reversed())
                .toList();
    }

    private List<RentabilidadResumenDto.GrupoProducto> porProducto(List<Rentabilidad> datos) {
        return datos.stream()
                .collect(Collectors.groupingBy(r -> r.getVenta().getProducto().getSku()))
                .values().stream()
                .map(lista -> {
                    var prod = lista.get(0).getVenta().getProducto();
                    BigDecimal ing = sum(lista, Rentabilidad::getIngresoNeto);
                    BigDecimal mar = sum(lista, Rentabilidad::getMargenBruto);
                    return new RentabilidadResumenDto.GrupoProducto(
                            prod.getSku(),
                            prod.getNombre(),
                            prod.getCategoria() != null ? prod.getCategoria() : "Sin categoría",
                            unidades(lista), ing,
                            sum(lista, Rentabilidad::getCostoTotal), mar, pct(mar, ing));
                })
                .sorted(Comparator.comparing(RentabilidadResumenDto.GrupoProducto::ingreso).reversed())
                .toList();
    }

    private List<RentabilidadResumenDto.GrupoMes> porMes(List<Rentabilidad> datos) {
        return datos.stream()
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getVenta().getFechaVenta()).toString()))
                .entrySet().stream()
                .map(e -> {
                    var lista = e.getValue();
                    BigDecimal ing = sum(lista, Rentabilidad::getIngresoNeto);
                    BigDecimal mar = sum(lista, Rentabilidad::getMargenBruto);
                    return new RentabilidadResumenDto.GrupoMes(
                            e.getKey(), unidades(lista), ing, mar, pct(mar, ing));
                })
                .sorted(Comparator.comparing(RentabilidadResumenDto.GrupoMes::mes))
                .toList();
    }

    private RentabilidadDetalleDto toDetalle(Rentabilidad r, Map<UUID, Map<String, BigDecimal>> costos) {
        var v = r.getVenta();
        var p = v.getProducto();

        // Desglose real del costo operacional (del estado de cuenta). Si la venta
        // todavía no está liquidada, el operacional viene de la estimación por comisión
        // de categoría (sin logística), así que lo mostramos todo como comisión.
        Map<String, BigDecimal> porTipo = costos.getOrDefault(v.getId(), Map.of());
        BigDecimal comisionReal = porTipo.getOrDefault("COMISION", BigDecimal.ZERO);
        BigDecimal logisticaReal = porTipo.getOrDefault("LOGISTICO", BigDecimal.ZERO);
        boolean hayReal = comisionReal.add(logisticaReal).signum() > 0;
        BigDecimal comision = hayReal ? comisionReal : r.getCostoOperacional();
        BigDecimal logistica = hayReal ? logisticaReal : BigDecimal.ZERO;

        return new RentabilidadDetalleDto(
                v.getId(),
                v.getFechaVenta(),
                v.getCanal().getNombre(),
                p.getSku(),
                p.getNombre(),
                p.getCategoria(),
                v.getPrecioVenta(),
                v.getDescuentoCampana(),
                r.getIngresoNeto(),
                r.getCostoProducto(),
                comision,
                logistica,
                r.getCostoOperacional(),
                r.getCostoTotal(),
                r.getMargenBruto(),
                r.getMargenPorcentaje());
    }

    /** Mapa ventaId -> (tipo -> monto) con los costos reales, en una sola consulta (evita N+1). */
    private Map<UUID, Map<String, BigDecimal>> costosPorVentaYTipo() {
        Map<UUID, Map<String, BigDecimal>> mapa = new HashMap<>();
        for (Object[] fila : costoVentaRepository.sumByVentaAndTipo()) {
            UUID ventaId = (UUID) fila[0];
            String tipo = (String) fila[1];
            BigDecimal monto = (BigDecimal) fila[2];
            mapa.computeIfAbsent(ventaId, k -> new HashMap<>()).put(tipo, monto);
        }
        return mapa;
    }

    private static String categoria(Rentabilidad r) {
        var c = r.getVenta().getProducto().getCategoria();
        return c != null ? c : "Sin categoría";
    }

    private static long unidades(List<Rentabilidad> lista) {
        return lista.stream()
                .mapToLong(r -> r.getVenta().getCantidad() != null ? r.getVenta().getCantidad() : 1)
                .sum();
    }

    private static BigDecimal sum(List<Rentabilidad> lista, Function<Rentabilidad, BigDecimal> campo) {
        return lista.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Margen ponderado del grupo: margen total / ingreso total * 100 (evita dividir por cero). */
    private static BigDecimal pct(BigDecimal margen, BigDecimal ingreso) {
        return ingreso.signum() == 0
                ? BigDecimal.ZERO
                : margen.divide(ingreso, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
