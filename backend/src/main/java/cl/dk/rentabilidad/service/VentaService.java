package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Servicio que gestiona el registro y consulta de ventas.
 *
 * Las ventas pueden ingresarse de dos formas:
 * 1. Importación de archivos CSV/Excel desde los marketplaces
 * 2. Registro manual para canales sin API disponible
 *
 * Cada vez que se registra una venta, se dispara automáticamente
 * el cálculo de rentabilidad para mantener los datos actualizados.
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final RentabilidadService rentabilidadService;

    /**
     * Registra una nueva venta y calcula su rentabilidad inmediatamente.
     *
     * @param venta entidad con los datos de la venta
     * @return venta persistida
     */
    @Transactional
    public Venta registrar(Venta venta) {

        // Persistimos la venta primero
        Venta ventaGuardada = ventaRepository.save(venta);

        // Calculamos y persistimos la rentabilidad inmediatamente
        // para mantener siempre los datos actualizados
        rentabilidadService.calcular(ventaGuardada);

        return ventaGuardada;
    }

    /**
     * Filtra ventas según los criterios del dashboard o del reporte Excel.
     * Todos los parámetros son opcionales: si son null se ignoran.
     *
     * @param desde     fecha inicio del rango (requerida)
     * @param hasta     fecha fin del rango (requerida)
     * @param canalId   UUID del canal a filtrar (opcional)
     * @param categoria categoría de producto a filtrar (opcional)
     * @return lista de ventas que cumplen los filtros
     */
    public List<Venta> filtrar(LocalDate desde,
                               LocalDate hasta,
                               UUID canalId,
                               String categoria) {
        return ventaRepository.filtrar(desde, hasta, canalId, categoria);
    }

    /**
     * Retorna todas las ventas de un canal específico.
     * Útil para la vista de detalle por canal.
     *
     * @param canalId UUID del canal
     * @return lista de ventas del canal
     */
    public List<Venta> listarPorCanal(UUID canalId) {
        return ventaRepository.findByCanalId(canalId);
    }
}