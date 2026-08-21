package cl.dk.rentabilidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Costo por SKU para MercadoLibre, cargado vía CSV.
 */
@Entity
@Table(name = "mercadolibre_costo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MercadoLibreCosto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "costo_prom", precision = 12, scale = 2)
    private BigDecimal costoProm;

    @Column(name = "ultimo_costo", precision = 12, scale = 2)
    private BigDecimal ultimoCosto;

    @Column(name = "costo_mercadolibre", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoMercadoLibre;

    @Column(name = "fuente_archivo", length = 255)
    private String fuenteArchivo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
