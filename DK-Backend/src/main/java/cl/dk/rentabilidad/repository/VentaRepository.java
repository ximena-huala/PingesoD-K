package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VentaRepository extends JpaRepository<Venta, UUID> {

    List<Venta> findByCanalId(UUID canalId);

    /** Para que la sincronización sea idempotente: una venta por OrderItemId del canal. */
    Optional<Venta> findByReferenciaExterna(String referenciaExterna);

    List<Venta> findByFechaVentaBetween(LocalDate desde, LocalDate hasta);

    @Query("""
        SELECT v FROM Venta v
        WHERE (:canalId IS NULL OR v.canal.id = :canalId)
          AND (:categoria IS NULL OR v.producto.categoria = :categoria)
          AND v.fechaVenta BETWEEN :desde AND :hasta
        """)
    List<Venta> filtrar(
            @Param("desde")     LocalDate desde,
            @Param("hasta")     LocalDate hasta,
            @Param("canalId")   UUID canalId,
            @Param("categoria") String categoria
    );

    /** Cuántas ventas hay de un canal (para el estado de la integración). */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.canal.nombre = :canal")
    long contarPorCanal(@Param("canal") String canal);

    /** Fecha de la venta más reciente de un canal (null si no hay ventas). */
    @Query("SELECT MAX(v.fechaVenta) FROM Venta v WHERE v.canal.nombre = :canal")
    LocalDate ultimaFechaPorCanal(@Param("canal") String canal);
}
