package cl.dk.rentabilidad.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de una ejecución de sincronización con una fuente externa.
 */
@Entity
@Table(name = "integracion_sync_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegracionSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "productos_creados", nullable = false)
    private Integer productosCreados = 0;

    @Column(name = "productos_actualizados", nullable = false)
    private Integer productosActualizados = 0;

    @Column(name = "productos_omitidos", nullable = false)
    private Integer productosOmitidos = 0;

    @Column(nullable = false)
    private Integer errores = 0;

    @Column(name = "detalle_errores", columnDefinition = "TEXT")
    private String detalleErrores;

    @Column(name = "iniciado_en", nullable = false, updatable = false)
    private LocalDateTime iniciadoEn;

    @Column(name = "finalizado_en")
    private LocalDateTime finalizadoEn;

    @PrePersist
    protected void onCreate() {
        if (this.iniciadoEn == null) {
            this.iniciadoEn = LocalDateTime.now();
        }
    }
}
