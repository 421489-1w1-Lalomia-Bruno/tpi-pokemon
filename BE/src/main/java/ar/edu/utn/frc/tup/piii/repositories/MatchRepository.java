package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {

    @Query("SELECT m FROM MatchEntity m WHERE m.status = :status AND m.player2Id IS NULL ORDER BY m.createdAt ASC")
    List<MatchEntity> findAvailableMatches(@Param("status") MatchStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MatchEntity m WHERE m.id = :id")
    Optional<MatchEntity> findByIdForUpdate(@Param("id") UUID id);
}
