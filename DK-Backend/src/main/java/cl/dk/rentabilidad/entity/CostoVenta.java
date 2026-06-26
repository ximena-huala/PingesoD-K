package cl.dk.rentabilidad.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un costo operacional real de una venta, tomado del estado de cuenta de Falabella.
 *
 * <p>Hay uno por tipo y por venta: la comisión, el cofinanciamiento logístico, etc.
 * El monto es la magnitud (positivo). La suma de estos es el costo operacional real
 * de la venta, y reemplaza a la estimación por tasas cuando existe.
 */
@Entity
@Table(name = "costo_venta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CostoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    @JsonIgnore
    private Venta venta;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Builder.Default
    @Column(nullable = false, length = 40)
    private String fuente = "ESTADO_CUENTA";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
