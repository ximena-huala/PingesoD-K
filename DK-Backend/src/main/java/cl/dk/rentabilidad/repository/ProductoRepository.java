package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {
    Optional<Producto> findBySku(String sku);
    List<Producto> findByActivoTrue();
    List<Producto> findByCategoria(String categoria);
}