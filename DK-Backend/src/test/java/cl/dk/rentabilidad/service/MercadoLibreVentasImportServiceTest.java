package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.dto.MercadoLibreImportResultDto;
import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.repository.CanalVentaRepository;
import cl.dk.rentabilidad.repository.CostoVentaRepository;
import cl.dk.rentabilidad.repository.ProductoRepository;
import cl.dk.rentabilidad.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoLibreVentasImportServiceTest {

    @Mock
    private CanalVentaRepository canalVentaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private CostoVentaRepository costoVentaRepository;

    @Mock
    private RentabilidadService rentabilidadService;

    @InjectMocks
    private MercadoLibreService mercadoLibreService;

    @Test
    void importarVentasDesdeCsv_creaVenta_yRecalculaRentabilidad() {
        String csv = String.join("\n",
                "orderId,fecha,sku,cantidad,precioVenta,comision,envio,categoria,producto",
                "ML-1001,2026-08-15,SKU-10,2,19990,600,0,Cosmetica,Crema",
                ""
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ventas-ml.csv",
                "text/csv",
                csv.getBytes()
        );

        CanalVenta canal = CanalVenta.builder()
                .id(UUID.randomUUID())
                .nombre("MercadoLibre")
                .tipo("MARKETPLACE")
                .activo(true)
                .build();

        Producto producto = Producto.builder()
                .id(UUID.randomUUID())
                .sku("SKU-10")
                .nombre("Crema")
                .categoria("Cosmetica")
                .costoBase(new BigDecimal("7800"))
                .activo(true)
                .stock(BigDecimal.ZERO)
                .build();

        when(canalVentaRepository.findByNombre("MercadoLibre")).thenReturn(Optional.of(canal));
        when(productoRepository.findBySku("SKU-10")).thenReturn(Optional.of(producto));
        when(ventaRepository.findByReferenciaExterna("ML-1001")).thenReturn(Optional.empty());
        when(costoVentaRepository.findByVentaId(any())).thenReturn(java.util.Collections.emptyList());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        MercadoLibreImportResultDto result = mercadoLibreService.importarVentasDesdeCsv(file);

        assertEquals(1, result.getCreados());
        assertEquals(1, result.getTotalProcesados());
        verify(ventaRepository).save(any(Venta.class));
        verify(rentabilidadService).calcular(any(Venta.class));
    }
}
