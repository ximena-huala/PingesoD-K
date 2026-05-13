package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.Rentabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentabilidadRepository extends JpaRepository<Rentabilidad, UUID> {

    Optional<Rentabilidad> findByVentaId(UUID ventaId);

    // Para el reporte con filtros
    @Query("""
        SELECT r FROM Rentabilidad r
        JOIN r.venta v
        WHERE (:canalId IS NULL OR v.canal.id = :canalId)
          AND (:categoria IS NULL OR v.producto.categoria = :categoria)
          AND v.fechaVenta BETWEEN :desde AND :hasta
        """)
    List<Rentabilidad> filtrar(
            @Param("desde")     LocalDate desde,
            @Param("hasta")     LocalDate hasta,
            @Param("canalId")   UUID canalId,
            @Param("categoria") String categoria
    );
}