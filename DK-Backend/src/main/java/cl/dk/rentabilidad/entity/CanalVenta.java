package cl.dk.rentabilidad.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Canal por el cual D&K vende sus productos.
 *
 * <p>Tipos válidos: {@code MARKETPLACE}, {@code TIENDA_WEB_PROPIA}, {@code TIENDA_FISICA}.
 */
@Entity
@Table(name = "canal_venta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanalVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo; // MARKETPLACE, TIENDA_WEB_PROPIA, TIENDA_FISICA

    @Column(nullable = false)
    private Boolean activo = true;
}