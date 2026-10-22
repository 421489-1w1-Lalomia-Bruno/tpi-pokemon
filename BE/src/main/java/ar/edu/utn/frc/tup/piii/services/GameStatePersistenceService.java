package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;

import java.util.UUID;

/**
 * Servicio responsable de cargar y sanitizar el MatchSnapshot persistido.
 * La implementación concreta puede residir en otro módulo/rol.
 */
public interface GameStatePersistenceService {

    /**
     * Carga el snapshot completo de la partida identificado por matchId.
     */
    MatchSnapshot loadSnapshot(UUID matchId);

    /**
     * Retorna una copia del snapshot filtrada desde la perspectiva del playerId.
     */
    MatchSnapshot sanitizeSnapshotForPlayer(MatchSnapshot snapshot, String playerId);

    /**
     * Persiste (crea o actualiza) el snapshot completo de la partida.
     */
    void saveSnapshot(MatchSnapshot snapshot);
}
