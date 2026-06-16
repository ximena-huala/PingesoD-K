package cl.dk.rentabilidad.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de una unidad vendida en un canal.
 *
 * <p>Una fila equivale a una unidad vendida. Al persistirse, dispara el cálculo
 * de {@link Rentabilidad} asociado.
 */
@Entity
@Table(name = "venta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canal_id", nullable = false)
    private CanalVenta canal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDate fechaVenta;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(name = "descuento_campana", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoCampana = BigDecimal.ZERO;

    @Column(name = "referencia_externa", length = 200)
    private String referenciaExterna;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}