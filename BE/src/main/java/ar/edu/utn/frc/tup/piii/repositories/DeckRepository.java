package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.enums.DeckSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeckRepository extends JpaRepository<DeckEntity, UUID> {
    List<DeckEntity> findByCreatorIdAndIsActive(String creatorId, Boolean isActive);
    List<DeckEntity> findBySourceAndIsActive(DeckSource source, Boolean isActive);
}
