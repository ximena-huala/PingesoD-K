package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.CostoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CostoVentaRepository extends JpaRepository<CostoVenta, UUID> {

    List<CostoVenta> findByVentaId(UUID ventaId);

    /** Costo operacional real de una venta = suma de sus costos del estado de cuenta. */
    @Query("SELECT COALESCE(SUM(c.monto), 0) FROM CostoVenta c WHERE c.venta.id = :ventaId")
    BigDecimal sumByVentaId(@Param("ventaId") UUID ventaId);
}
