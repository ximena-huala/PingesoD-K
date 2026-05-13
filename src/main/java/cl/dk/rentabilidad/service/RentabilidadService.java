package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.entity.Rentabilidad;
import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.repository.CostoCanalRepository;
import cl.dk.rentabilidad.repository.RentabilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

        // 3. Costos operacionales vigentes del canal a la fecha de la venta
        //    (comisiones, envío, logística, etc. configurados manualmente)
        BigDecimal costoOperacional = calcularCostoOperacional(
                venta.getCanal().getId(),
                venta.getFechaVenta(),
                ingresoNeto
        );

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

    /**
     * Calcula el costo operacional total para un canal en una fecha dada.
     *
     * Itera sobre todos los costos vigentes del canal y los aplica según
     * su tipo: si es porcentual lo calcula sobre el ingreso neto,
     * si es monto fijo lo suma directamente.
     *
     * @param canalId    UUID del canal de venta
     * @param fecha      fecha de la venta (determina qué costos estaban vigentes)
     * @param base       ingreso neto sobre el cual se calculan los porcentajes
     * @return suma total de costos operacionales
     */
    private BigDecimal calcularCostoOperacional(UUID canalId,
                                                java.time.LocalDate fecha,
                                                BigDecimal base) {

        // Obtenemos solo los costos vigentes a la fecha de la venta
        List<CostoCanal> costos = costoCanalRepository
                .findVigentesByCanalAndFecha(canalId, fecha);

        return costos.stream()
                .map(costo -> {
                    if (costo.getEsPorcentaje()) {
                        // Costo porcentual: ej. comisión MercadoLibre 13%
                        return base.multiply(costo.getValor())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    } else {
                        // Costo fijo: ej. costo de envío $2.500
                        return costo.getValor();
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
