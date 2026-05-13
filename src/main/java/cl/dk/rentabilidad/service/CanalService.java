package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.repository.CanalVentaRepository;
import cl.dk.rentabilidad.repository.CostoCanalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio que gestiona los canales de venta y sus costos operacionales.
 *
 * Los costos se editan manualmente por el equipo de D&K desde la aplicación,
 * por lo que este servicio es crítico para mantener la configuración actualizada.
 */
@Service
@RequiredArgsConstructor
public class CanalService {

    private final CanalVentaRepository canalVentaRepository;
    private final CostoCanalRepository costoCanalRepository;

    /**
     * Retorna todos los canales activos disponibles.
     */
    public List<CanalVenta> listarActivos() {
        return canalVentaRepository.findByActivoTrue();
    }

    /**
     * Retorna todos los canales sin importar si están activos o no.
     * Útil para la pantalla de configuración de administrador.
     */
    public List<CanalVenta> listarTodos() {
        return canalVentaRepository.findAll();
    }

    /**
     * Crea un nuevo canal de venta.
     *
     * @param canal entidad con los datos del nuevo canal
     * @return canal persistido con su ID generado
     */
    @Transactional
    public CanalVenta crear(CanalVenta canal) {
        return canalVentaRepository.save(canal);
    }

    /**
     * Agrega un costo operacional a un canal existente.
     * Los costos tienen fecha de inicio y fin para historizar cambios,
     * ya que las comisiones de los marketplaces pueden cambiar con el tiempo.
     *
     * @param canalId UUID del canal al que se agrega el costo
     * @param costo   entidad con los datos del costo a agregar
     * @return costo persistido
     * @throws RuntimeException si el canal no existe
     */
    @Transactional
    public CostoCanal agregarCosto(UUID canalId, CostoCanal costo) {

        // Verificamos que el canal exista antes de asociar el costo
        CanalVenta canal = canalVentaRepository.findById(canalId)
                .orElseThrow(() -> new RuntimeException("Canal no encontrado: " + canalId));

        costo.setCanal(canal);
        return costoCanalRepository.save(costo);
    }

    /**
     * Lista todos los costos configurados para un canal específico.
     * Incluye costos vencidos para mantener el historial.
     *
     * @param canalId UUID del canal
     * @return lista de costos ordenados por fecha de inicio
     */
    public List<CostoCanal> listarCostos(UUID canalId) {
        return costoCanalRepository.findByCanalId(canalId);
    }
}