package cl.dk.rentabilidad.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Producto del catálogo maestro de D&K.
 *
 * <p>Mapea la tabla {@code producto}. El {@link #sku} es único en toda la empresa
 * y el {@link #costoBase} representa el costo de adquisición o fabricación.
 */
@Entity
@Table(name = "producto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(length = 100)
    private String categoria;

    @Column(length = 100)
    private String marca;

    @Column(name = "tipo_producto", length = 100)
    private String tipoProducto;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(name = "costo_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoBase;

    /** ID de la variante en Bsale (SKU maestro). */
    @Column(name = "bsale_variant_id", unique = true)
    private Integer bsaleVariantId;

    /** ID del producto padre en Bsale. */
    @Column(name = "bsale_product_id")
    private Integer bsaleProductId;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}