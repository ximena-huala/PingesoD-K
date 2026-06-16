package cl.dk.rentabilidad.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado del cálculo de margen para una venta.
 *
 * <p>Relación 1:1 con {@link Venta}. Los valores se derivan de:
 * ingreso neto, costo del producto y costos operacionales vigentes del canal.
 */
@Entity
@Table(name = "rentabilidad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rentabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false, unique = true)
    private Venta venta;

    @Column(name = "ingreso_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresoNeto;

    @Column(name = "costo_producto", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoProducto;

    @Column(name = "costo_operacional", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoOperacional;

    @Column(name = "costo_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoTotal;

    @Column(name = "margen_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal margenBruto;

    @Column(name = "margen_porcentaje", nullable = false, precision = 8, scale = 4)
    private BigDecimal margenPorcentaje;

    @Column(name = "calculado_en", nullable = false)
    private LocalDateTime calculadoEn;

    @PrePersist
    @PreUpdate
    protected void onCalculate() {
        this.calculadoEn = LocalDateTime.now();
    }
}