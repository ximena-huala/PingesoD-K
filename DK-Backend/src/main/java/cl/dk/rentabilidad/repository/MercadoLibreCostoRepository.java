package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.MercadoLibreCosto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MercadoLibreCostoRepository extends JpaRepository<MercadoLibreCosto, UUID> {

    Optional<MercadoLibreCosto> findBySku(String sku);
}
