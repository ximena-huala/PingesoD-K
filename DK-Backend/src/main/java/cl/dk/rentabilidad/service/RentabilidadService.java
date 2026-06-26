package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.entity.Rentabilidad;
import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.repository.CostoCanalRepository;
import cl.dk.rentabilidad.repository.CostoVentaRepository;
import cl.dk.rentabilidad.repository.RentabilidadRepository;
import cl.dk.rentabilidad.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio encargado del cálculo de rentabilidad por venta.
 *
 * Lógica central del sistema: dado una venta, obtiene los costos
 * operacionales vigentes del canal y calcula el margen real.
 *
 * Fórmulas aplicadas (acuerdo con cliente D&K):
 *   - ingresoNeto       = precioVenta - descuentoCampana
 *   - costoOperacional  = suma de costos vigentes del canal a la fecha de venta
 *   - costoTotal        = costoProducto + costoOperacional
 *   - margenBruto       = ingresoNeto - costoTotal
 *   - margenPorcentaje  = (margenBruto / ingresoNeto) * 100
 *
 * IMPORTANTE: El cálculo es unitario por producto según lo acordado con el
 * cliente. No se distribuyen costos compartidos entre múltiples productos
 * de una misma orden.
 */
@Service
@RequiredArgsConstructor
public class RentabilidadService {

    private final CostoCanalRepository costoCanalRepository;
    private final RentabilidadRepository rentabilidadRepository;
    private final VentaRepository ventaRepository;
    private final CostoVentaRepository costoVentaRepository;

    /**
     * Recalcula la rentabilidad de todas las ventas registradas.
     *
     * Es el "recalcular cuando cambian los costos" del que habla el proyecto:
     * después de configurar o ajustar costo_canal (ej. la comisión de Falabella),
     * esto vuelve a calcular el margen de cada venta con los costos vigentes.
     * Va en una sola transacción para que las ventas queden gestionadas y se
     * puedan leer producto y canal sin problemas.
     *
     * @return cantidad de ventas recalculadas
     */
    @Transactional
    public int recalcularTodas() {
        List<Venta> ventas = ventaRepository.findAll();
        ventas.forEach(this::calcular);
        return ventas.size();
    }

    /**
     * Calcula y persiste la rentabilidad de una venta.
     * Si ya existe un registro previo para esa venta, lo sobreescribe
     * para reflejar cambios en costos operacionales.
     *
     * @param venta la venta sobre la que se calculará el margen
     * @return entidad Rentabilidad con todos los valores calculados
     */
    @Transactional
    public Rentabilidad calcular(Venta venta) {

        // 1. Ingreso neto: precio de venta menos descuentos de campaña (CyberDay, etc.)
        BigDecimal ingresoNeto = venta.getPrecioVenta()
                .subtract(venta.getDescuentoCampana());

        // 2. Costo del producto: lo que le costó a D&K comprar/fabricar el producto
        BigDecimal costoProducto = venta.getProducto().getCostoBase();

        // 3. Costos operacionales. Preferimos los REALES del estado de cuenta
        //    (comisión + logística + etc. por orden); si la venta todavía no está
        //    en el estado de cuenta, caemos a la estimación por tasa de categoría.
        BigDecimal costoReal = venta.getId() == null
                ? BigDecimal.ZERO
                : costoVentaRepository.sumByVentaId(venta.getId());
        BigDecimal costoOperacional = costoReal.compareTo(BigDecimal.ZERO) > 0
                ? costoReal
                : calcularCostoOperacional(
                        venta.getCanal().getId(),
                        venta.getProducto().getCategoria(),
                        venta.getFechaVenta(),
                        ingresoNeto);

        // 4. Costo total = costo del producto + todos los costos operacionales
        BigDecimal costoTotal = costoProducto.add(costoOperacional);

        // 5. Margen bruto = lo que realmente ganó D&K en esta venta
        BigDecimal margenBruto = ingresoNeto.subtract(costoTotal);

        // 6. Margen porcentual sobre el ingreso neto (evitamos división por cero)
        BigDecimal margenPorcentaje = ingresoNeto.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : margenBruto.divide(ingresoNeto, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // 7. Buscamos si ya existe un registro de rentabilidad para esta venta
        //    para actualizarlo en vez de crear uno duplicado
        Rentabilidad rentabilidad = rentabilidadRepository
                .findByVentaId(venta.getId())
                .orElse(Rentabilidad.builder().venta(venta).build());

        // 8. Asignamos todos los valores calculados
        rentabilidad.setIngresoNeto(ingresoNeto);
        rentabilidad.setCostoProducto(costoProducto);
        rentabilidad.setCostoOperacional(costoOperacional);
        rentabilidad.setCostoTotal(costoTotal);
        rentabilidad.setMargenBruto(margenBruto);
        rentabilidad.setMargenPorcentaje(margenPorcentaje);

        // 9. Persistimos y retornamos el resultado
        return rentabilidadRepository.save(rentabilidad);
    }

    @Transactional
    public void eliminarPorVenta(UUID ventaId) {
        rentabilidadRepository.findByVentaId(ventaId)
                .ifPresent(rentabilidadRepository::delete);
    }

    /**
     * Calcula el costo operacional total para un canal, considerando la categoría
     * del producto.
     *
     * Trae los costos vigentes que aplican (los específicos de la categoría más los
     * del canal sin categoría) y, para cada tipo de costo, se queda con el más
     * específico: si hay una comisión propia de la categoría, usa esa; si no, usa la
     * del canal. Luego suma: los porcentuales sobre el ingreso neto y los fijos tal cual.
     *
     * @param canalId    UUID del canal de venta
     * @param categoria  categoría del producto (define qué comisión aplica)
     * @param fecha      fecha de la venta (determina qué costos estaban vigentes)
     * @param base       ingreso neto sobre el cual se calculan los porcentajes
     * @return suma total de costos operacionales
     */
    private BigDecimal calcularCostoOperacional(UUID canalId,
                                                String categoria,
                                                java.time.LocalDate fecha,
                                                BigDecimal base) {

        List<CostoCanal> aplicables = costoCanalRepository
                .findAplicables(canalId, categoria, fecha);

        // Por cada tipo de costo, preferir el específico de la categoría sobre el
        // general (categoria == null), para no sumar dos comisiones al mismo producto.
        Map<String, CostoCanal> elegidoPorTipo = new HashMap<>();
        for (CostoCanal c : aplicables) {
            CostoCanal actual = elegidoPorTipo.get(c.getTipoCosto());
            boolean reemplaza = actual == null
                    || (c.getCategoria() != null && actual.getCategoria() == null);
            if (reemplaza) elegidoPorTipo.put(c.getTipoCosto(), c);
        }

        return elegidoPorTipo.values().stream()
                .map(costo -> costo.getEsPorcentaje()
                        ? base.multiply(costo.getValor())
                              .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        : costo.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
