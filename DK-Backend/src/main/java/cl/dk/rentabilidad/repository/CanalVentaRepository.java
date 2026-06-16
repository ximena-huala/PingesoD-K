package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.CanalVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CanalVentaRepository extends JpaRepository<CanalVenta, UUID> {
    List<CanalVenta> findByActivoTrue();
    Optional<CanalVenta> findByNombre(String nombre);
}