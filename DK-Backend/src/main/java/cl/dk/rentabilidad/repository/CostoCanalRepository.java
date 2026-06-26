package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.CostoCanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CostoCanalRepository extends JpaRepository<CostoCanal, UUID> {

    // Costos vigentes para un canal en una fecha dada
    @Query("""
        SELECT c FROM CostoCanal c
        WHERE c.canal.id = :canalId
          AND c.fechaInicio <= :fecha
          AND (c.fechaFin IS NULL OR c.fechaFin >= :fecha)
        """)
    List<CostoCanal> findVigentesByCanalAndFecha(
            @Param("canalId") UUID canalId,
            @Param("fecha") LocalDate fecha
    );

    /**
     * Costos vigentes que aplican a un producto de una categoría dada: los
     * específicos de su categoría y los del canal sin categoría (el default).
     * El servicio se queda con el más específico por tipo de costo.
     */
    @Query("""
        SELECT c FROM CostoCanal c
        WHERE c.canal.id = :canalId
          AND c.fechaInicio <= :fecha
          AND (c.fechaFin IS NULL OR c.fechaFin >= :fecha)
          AND (c.categoria IS NULL OR c.categoria = :categoria)
        """)
    List<CostoCanal> findAplicables(
            @Param("canalId") UUID canalId,
            @Param("categoria") String categoria,
            @Param("fecha") LocalDate fecha
    );

    List<CostoCanal> findByCanalId(UUID canalId);

    boolean existsByIdAndCanal_Id(UUID id, UUID canalId);
}