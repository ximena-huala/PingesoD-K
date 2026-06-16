package cl.dk.rentabilidad.repository;

import cl.dk.rentabilidad.entity.IntegracionSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegracionSyncLogRepository extends JpaRepository<IntegracionSyncLog, UUID> {

    Optional<IntegracionSyncLog> findFirstByFuenteOrderByIniciadoEnDesc(String fuente);
}
