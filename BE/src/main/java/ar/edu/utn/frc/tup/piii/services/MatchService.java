package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.match.CreateMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.JoinMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.MatchDTO;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;

import java.util.List;
import java.util.UUID;

public interface MatchService {

    List<MatchDTO> listAvailableMatches(String playerId);

    MatchDTO getMatch(String playerId, UUID matchId);

    MatchDTO createMatch(String playerId, CreateMatchDTO dto);

    MatchDTO joinMatch(String playerId, UUID matchId, JoinMatchDTO dto);

    void leaveMatch(String playerId, UUID matchId);

    void cancelMatch(String playerId, UUID matchId);

    MatchSnapshot getMatchState(String playerId, UUID matchId);

}
