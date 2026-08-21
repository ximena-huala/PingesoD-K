package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.CostoCanal;
import cl.dk.rentabilidad.exception.ConflictException;
import cl.dk.rentabilidad.exception.ResourceNotFoundException;
import cl.dk.rentabilidad.repository.CanalVentaRepository;
import cl.dk.rentabilidad.repository.CostoCanalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link CanalService}: gestión de canales de venta y sus costos
 * (comisiones/logística por canal), la fuente de estimación que usa
 * {@link RentabilidadService} y que edita el frontend vía {@code /api/canales}.
 */
@ExtendWith(MockitoExtension.class)
class CanalServiceTest {

    @Mock CanalVentaRepository canalVentaRepository;
    @Mock CostoCanalRepository costoCanalRepository;

    @InjectMocks CanalService service;

    // ── Canales ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Crea un canal: recorta el nombre y lo deja activo por defecto")
    void creaCanalRecortaNombreYActivaPorDefecto() {
        when(canalVentaRepository.findByNombre("Falabella")).thenReturn(Optional.empty());
        when(canalVentaRepository.save(any(CanalVenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CanalVenta creado = service.crear(
                CanalVenta.builder().nombre("  Falabella  ").tipo("MARKETPLACE").build());

        assertEquals("Falabella", creado.getNombre());   // recortado
        assertTrue(creado.getActivo());                  // activo por defecto
    }

    @Test
    @DisplayName("No permite dos canales con el mismo nombre")
    void rechazaCanalDuplicado() {
        when(canalVentaRepository.findByNombre("Falabella"))
                .thenReturn(Optional.of(CanalVenta.builder().nombre("Falabella").build()));

        assertThrows(ConflictException.class, () -> service.crear(
                CanalVenta.builder().nombre("Falabella").tipo("MARKETPLACE").build()));

        verify(canalVentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exige nombre y tipo al crear un canal")
    void exigeNombreYTipo() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                CanalVenta.builder().nombre("").tipo("MARKETPLACE").build()));
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                CanalVenta.builder().nombre("Falabella").tipo(null).build()));
    }

    @Test
    @DisplayName("Obtener un canal inexistente lanza ResourceNotFoundException")
    void obtenerCanalInexistente() {
        UUID id = UUID.randomUUID();
        when(canalVentaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(id));
    }

    @Test
    @DisplayName("Desactivar un canal lo marca inactivo sin borrarlo")
    void desactivaCanal() {
        UUID id = UUID.randomUUID();
        CanalVenta canal = CanalVenta.builder().id(id).nombre("Falabella").tipo("MARKETPLACE").activo(true).build();
        when(canalVentaRepository.findById(id)).thenReturn(Optional.of(canal));
        when(canalVentaRepository.save(any(CanalVenta.class))).thenAnswer(inv -> inv.getArgument(0));

        service.desactivar(id);

        assertFalse(canal.getActivo());
        verify(canalVentaRepository).save(canal);
    }

    // ── Costos de canal (comisiones / logística) ────────────────────────────────

    @Test
    @DisplayName("Agrega un costo de comisión y lo asocia al canal")
    void agregaCostoDeComision() {
        UUID canalId = UUID.randomUUID();
        CanalVenta canal = CanalVenta.builder().id(canalId).nombre("Falabella").tipo("MARKETPLACE").build();
        when(canalVentaRepository.findById(canalId)).thenReturn(Optional.of(canal));
        when(costoCanalRepository.save(any(CostoCanal.class))).thenAnswer(inv -> inv.getArgument(0));

        CostoCanal guardado = service.agregarCosto(canalId, CostoCanal.builder()
                .tipoCosto("COMISION_PORCENTAJE").valor(new BigDecimal("18")).esPorcentaje(true).build());

        assertEquals(canal, guardado.getCanal());               // se asocia al canal
        assertEquals(0, new BigDecimal("18").compareTo(guardado.getValor()));
    }

    @Test
    @DisplayName("Exige tipo y valor al agregar un costo")
    void exigeTipoYValorEnCosto() {
        UUID canalId = UUID.randomUUID();
        when(canalVentaRepository.findById(canalId))
                .thenReturn(Optional.of(CanalVenta.builder().id(canalId).build()));

        assertThrows(IllegalArgumentException.class, () -> service.agregarCosto(canalId,
                CostoCanal.builder().tipoCosto("COMISION_PORCENTAJE").build()));   // sin valor

        verify(costoCanalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un costo que no pertenece al canal no se encuentra")
    void costoDeOtroCanalNoSeEncuentra() {
        UUID canalId = UUID.randomUUID();
        UUID costoId = UUID.randomUUID();
        when(costoCanalRepository.existsByIdAndCanal_Id(costoId, canalId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerCosto(canalId, costoId));
    }

    @Test
    @DisplayName("Elimina un costo existente del canal")
    void eliminaCosto() {
        UUID canalId = UUID.randomUUID();
        UUID costoId = UUID.randomUUID();
        CostoCanal costo = CostoCanal.builder().tipoCosto("COMISION_PORCENTAJE").valor(new BigDecimal("18")).build();
        when(costoCanalRepository.existsByIdAndCanal_Id(costoId, canalId)).thenReturn(true);
        when(costoCanalRepository.findById(costoId)).thenReturn(Optional.of(costo));

        service.eliminarCosto(canalId, costoId);

        verify(costoCanalRepository).delete(costo);
    }
}
