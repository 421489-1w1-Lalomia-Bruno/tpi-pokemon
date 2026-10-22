package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, UUID>, JpaSpecificationExecutor<CardEntity> {
    List<CardEntity> findBySetId(String setId);
    Page<CardEntity> findByStage(PokemonStage stage, Pageable pageable);
    Page<CardEntity> findBySupertype(String supertype, Pageable pageable);
    Optional<CardEntity> findByExternalId(String externalId);
    List<CardEntity> findByNameContainingIgnoreCase(String name);
    Page<CardEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<CardEntity> findBySetIdAndSupertype(String setId, String supertype, Pageable pageable);

    @Query("SELECT c.externalId FROM CardEntity c WHERE c.setId = :setId")
    Set<String> findAllExternalIdsBySetId(@Param("setId") String setId);
}
