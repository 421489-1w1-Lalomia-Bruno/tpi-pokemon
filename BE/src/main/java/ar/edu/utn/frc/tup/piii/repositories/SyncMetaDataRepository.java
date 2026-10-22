package ar.edu.utn.frc.tup.piii.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ar.edu.utn.frc.tup.piii.entities.SyncMetaDataEntity;

public interface SyncMetaDataRepository extends JpaRepository<SyncMetaDataEntity, UUID> {
    Optional<SyncMetaDataEntity> findBySetId(String setId);
}