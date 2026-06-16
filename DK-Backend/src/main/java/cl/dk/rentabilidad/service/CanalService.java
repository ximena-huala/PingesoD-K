package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.exception.ConflictException;
import cl.dk.rentabilidad.exception.ResourceNotFoundException;
import cl.dk.rentabilidad.repository.CanalVentaRepository;
import cl.dk.rentabilidad.repository.CostoCanalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestiona canales de venta (marketplaces, tiendas web, tienda física)
 * y sus costos operacionales asociados.
 *
 * <p>Los costos vigentes a una fecha determinan el {@code costo_operacional}
 * que usa {@link RentabilidadService} al calcular el margen de cada venta.
 */
@Service
@RequiredArgsConstructor
public class CanalService {

    private final CanalVentaRepository canalVentaRepository;
    private final CostoCanalRepository costoCanalRepository;

    public List<CanalVenta> listarActivos() {
        return canalVentaRepository.findByActivoTrue();
    }

    public List<CanalVenta> listarTodos() {
        return canalVentaRepository.findAll();
    }

    public CanalVenta obtenerPorId(UUID id) {
        return canalVentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Canal no encontrado: " + id));
    }

    @Transactional
    public CanalVenta crear(CanalVenta canal) {
        validarCanal(canal);

        canalVentaRepository.findByNombre(canal.getNombre().trim()).ifPresent(c -> {
            throw new ConflictException("Ya existe un canal con nombre: " + canal.getNombre());
        });

        canal.setNombre(canal.getNombre().trim());
        canal.setActivo(canal.getActivo() != null ? canal.getActivo() : true);
        return canalVentaRepository.save(canal);
    }

    @Transactional
    public CanalVenta actualizar(UUID id, CanalVenta datos) {
        validarCanal(datos);

        CanalVenta existente = obtenerPorId(id);
        String nuevoNombre = datos.getNombre().trim();

        if (!existente.getNombre().equals(nuevoNombre)) {
            canalVentaRepository.findByNombre(nuevoNombre).ifPresent(c -> {
                throw new ConflictException("Ya existe un canal con nombre: " + nuevoNombre);
            });
        }

        existente.setNombre(nuevoNombre);
        existente.setTipo(datos.getTipo());
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }

        return canalVentaRepository.save(existente);
    }

    /** Desactiva el canal sin eliminar su historial de ventas ni costos. */
    @Transactional
    public void desactivar(UUID id) {
        CanalVenta canal = obtenerPorId(id);
        canal.setActivo(false);
        canalVentaRepository.save(canal);
    }

    @Transactional
    public CostoCanal agregarCosto(UUID canalId, CostoCanal costo) {
        CanalVenta canal = obtenerPorId(canalId);
        validarCosto(costo);

        costo.setCanal(canal);
        return costoCanalRepository.save(costo);
    }

    public List<CostoCanal> listarCostos(UUID canalId) {
        obtenerPorId(canalId);
        return costoCanalRepository.findByCanalId(canalId);
    }

    public CostoCanal obtenerCosto(UUID canalId, UUID costoId) {
        if (!costoCanalRepository.existsByIdAndCanal_Id(costoId, canalId)) {
            throw new ResourceNotFoundException(
                    "Costo no encontrado: " + costoId + " en canal " + canalId);
        }

        return costoCanalRepository.findById(costoId)
                .orElseThrow(() -> new ResourceNotFoundException("Costo no encontrado: " + costoId));
    }

    @Transactional
    public CostoCanal actualizarCosto(UUID canalId, UUID costoId, CostoCanal datos) {
        CostoCanal existente = obtenerCosto(canalId, costoId);
        validarCosto(datos);

        existente.setTipoCosto(datos.getTipoCosto());
        existente.setDescripcion(datos.getDescripcion());
        existente.setValor(datos.getValor());
        existente.setEsPorcentaje(datos.getEsPorcentaje() != null ? datos.getEsPorcentaje() : true);
        existente.setFechaInicio(datos.getFechaInicio());
        existente.setFechaFin(datos.getFechaFin());

        return costoCanalRepository.save(existente);
    }

    @Transactional
    public void eliminarCosto(UUID canalId, UUID costoId) {
        CostoCanal costo = obtenerCosto(canalId, costoId);
        costoCanalRepository.delete(costo);
    }

    private void validarCanal(CanalVenta canal) {
        if (canal.getNombre() == null || canal.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del canal es obligatorio");
        }
        if (canal.getTipo() == null || canal.getTipo().isBlank()) {
            throw new IllegalArgumentException("El tipo de canal es obligatorio");
        }
    }

    private void validarCosto(CostoCanal costo) {
        if (costo.getTipoCosto() == null || costo.getTipoCosto().isBlank()) {
            throw new IllegalArgumentException("El tipo de costo es obligatorio");
        }
        if (costo.getValor() == null) {
            throw new IllegalArgumentException("El valor del costo es obligatorio");
        }
    }
}
