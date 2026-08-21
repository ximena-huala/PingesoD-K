package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.entity.Rentabilidad;
import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.repository.CostoCanalRepository;
import cl.dk.rentabilidad.repository.CostoVentaRepository;
import cl.dk.rentabilidad.repository.RentabilidadRepository;
import cl.dk.rentabilidad.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del motor de cálculo de rentabilidad ({@link RentabilidadService}),
 * la funcionalidad crítica del sistema.
 *
 * Verifican la fórmula del margen y las dos vías de costo operacional:
 *   - costos REALES del estado de cuenta ({@code costo_venta}) cuando existen;
 *   - estimación por tasa de categoría ({@code costo_canal}) como respaldo.
 */
@ExtendWith(MockitoExtension.class)
class RentabilidadServiceTest {

    @Mock CostoCanalRepository costoCanalRepository;
    @Mock RentabilidadRepository rentabilidadRepository;
    @Mock VentaRepository ventaRepository;
    @Mock CostoVentaRepository costoVentaRepository;

    @InjectMocks RentabilidadService service;

    @BeforeEach
    void setUp() {
        // Toda venta es nueva (no hay rentabilidad previa) y save devuelve lo que recibe.
        when(rentabilidadRepository.findByVentaId(any())).thenReturn(Optional.empty());
        when(rentabilidadRepository.save(any(Rentabilidad.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Vía 1: costos reales del estado de cuenta ──────────────────────────────

    @Test
    @DisplayName("Usa los costos reales del estado de cuenta cuando existen")
    void usaCostosRealesCuandoExisten() {
        // precio 50.000, sin descuento, costo producto 20.000, costos reales 10.000
        when(costoVentaRepository.sumByVentaId(any())).thenReturn(new BigDecimal("10000"));

        Rentabilidad r = service.calcular(venta("50000", "0", "20000", "Relojes"));

        assertMonto("50000", r.getIngresoNeto());
        assertMonto("20000", r.getCostoProducto());
        assertMonto("10000", r.getCostoOperacional());   // real, no estimado
        assertMonto("30000", r.getCostoTotal());
        assertMonto("20000", r.getMargenBruto());
        assertMonto("40.0000", r.getMargenPorcentaje()); // 20.000 / 50.000

        // Si hay costo real, NO se estima por categoría.
        verify(costoCanalRepository, never()).findAplicables(any(), any(), any());
    }

    @Test
    @DisplayName("Descuenta el descuento de campaña del ingreso neto")
    void descuentaDescuentoDeCampana() {
        when(costoVentaRepository.sumByVentaId(any())).thenReturn(new BigDecimal("5000"));

        Rentabilidad r = service.calcular(venta("50000", "10000", "20000", "Relojes"));

        assertMonto("40000", r.getIngresoNeto());   // 50.000 - 10.000
        assertMonto("15000", r.getMargenBruto());   // 40.000 - (20.000 + 5.000)
    }

    // ── Vía 2: estimación por tasa de categoría (respaldo) ─────────────────────

    @Test
    @DisplayName("Estima la comisión por porcentaje del canal cuando no hay estado de cuenta")
    void estimaComisionPorcentualCuandoNoHayCostoReal() {
        when(costoVentaRepository.sumByVentaId(any())).thenReturn(BigDecimal.ZERO);
        when(costoCanalRepository.findAplicables(any(), any(), any()))
                .thenReturn(List.of(comision("18", null))); // 18% general

        Rentabilidad r = service.calcular(venta("50000", "0", "20000", "Relojes"));

        assertMonto("9000", r.getCostoOperacional());    // 50.000 * 18%
        assertMonto("29000", r.getCostoTotal());
        assertMonto("21000", r.getMargenBruto());
        assertMonto("42.0000", r.getMargenPorcentaje());
    }

    @Test
    @DisplayName("Prefiere la comisión específica de la categoría sobre la general")
    void prefiereComisionDeCategoriaSobreGeneral() {
        when(costoVentaRepository.sumByVentaId(any())).thenReturn(BigDecimal.ZERO);
        when(costoCanalRepository.findAplicables(any(), any(), any()))
                .thenReturn(List.of(
                        comision("18", null),          // general
                        comision("12", "Relojes")));   // específica de la categoría

        Rentabilidad r = service.calcular(venta("50000", "0", "20000", "Relojes"));

        assertMonto("6000", r.getCostoOperacional());    // usa 12%, no 18%
    }

    @Test
    @DisplayName("Suma costos porcentuales y fijos del canal")
    void sumaCostosPorcentualesYFijos() {
        when(costoVentaRepository.sumByVentaId(any())).thenReturn(BigDecimal.ZERO);
        when(costoCanalRepository.findAplicables(any(), any(), any()))
                .thenReturn(List.of(
                        comision("18", null),             // 50.000 * 18% = 9.000
                        costoFijo("LOGISTICO", "1000"))); // + 1.000 fijo

        Rentabilidad r = service.calcular(venta("50000", "0", "20000", "Relojes"));

        assertMonto("10000", r.getCostoOperacional());   // 9.000 + 1.000
    }

    // ── Casos borde ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ingreso neto cero no rompe el cálculo (margen 0, sin división por cero)")
    void ingresoNetoCeroNoRompe() {
        when(costoVentaRepository.sumByVentaId(any())).thenReturn(new BigDecimal("5000"));

        Rentabilidad r = service.calcular(venta("0", "0", "0", "Relojes"));

        assertMonto("0", r.getIngresoNeto());
        assertMonto("0", r.getMargenPorcentaje());   // guardado, sin ArithmeticException
    }

    @Test
    @DisplayName("Detecta producto en pérdida (margen negativo)")
    void detectaProductoEnPerdida() {
        when(costoVentaRepository.sumByVentaId(any())).thenReturn(new BigDecimal("3000"));

        Rentabilidad r = service.calcular(venta("10000", "0", "8000", "Relojes"));

        assertMonto("-1000", r.getMargenBruto());    // 10.000 - (8.000 + 3.000)
        assertTrue(r.getMargenBruto().signum() < 0, "el margen debe ser negativo");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Venta venta(String precio, String descuento, String costoProducto, String categoria) {
        Producto p = Producto.builder()
                .sku("SKU-TEST")
                .nombre("Producto de prueba")
                .categoria(categoria)
                .costoBase(new BigDecimal(costoProducto))
                .build();
        CanalVenta canal = CanalVenta.builder().id(UUID.randomUUID()).nombre("Falabella").build();
        return Venta.builder()
                .id(UUID.randomUUID())
                .canal(canal)
                .producto(p)
                .fechaVenta(LocalDate.of(2026, 1, 15))
                .precioVenta(new BigDecimal(precio))
                .descuentoCampana(new BigDecimal(descuento))
                .build();
    }

    private CostoCanal comision(String porcentaje, String categoria) {
        return CostoCanal.builder()
                .tipoCosto("COMISION")
                .categoria(categoria)
                .valor(new BigDecimal(porcentaje))
                .esPorcentaje(true)
                .build();
    }

    private CostoCanal costoFijo(String tipo, String monto) {
        return CostoCanal.builder()
                .tipoCosto(tipo)
                .categoria(null)
                .valor(new BigDecimal(monto))
                .esPorcentaje(false)
                .build();
    }

    /** Compara montos ignorando la escala (2 vs 2.00). */
    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                () -> "esperado " + esperado + " pero fue " + real);
    }
}
