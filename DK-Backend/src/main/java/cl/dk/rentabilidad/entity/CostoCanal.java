package cl.dk.rentabilidad.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Costo operacional asociado a un canal de venta.
 *
 * <p>Puede ser porcentual (comisión) o monto fijo (envío). La vigencia se controla
 * con {@link #fechaInicio} y {@link #fechaFin}; {@code fechaFin = null} indica costo vigente.
 */
@Entity
@Table(name = "costo_canal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CostoCanal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canal_id", nullable = false)
    @JsonIgnore
    private CanalVenta canal;

    @Column(name = "tipo_costo", nullable = false, length = 50)
    private String tipoCosto;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal valor;

    @Column(name = "es_porcentaje", nullable = false)
    private Boolean esPorcentaje = true;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.fechaInicio == null) this.fechaInicio = LocalDate.now();
    }
}