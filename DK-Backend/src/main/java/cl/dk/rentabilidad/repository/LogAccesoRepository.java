package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.LogAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LogAccesoRepository extends JpaRepository<LogAcceso, UUID> {
}
