package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.match.CreateMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.JoinMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.MatchDTO;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public ResponseEntity<List<MatchDTO>> listMatches(@RequestAttribute("playerId") String playerId) {
        return ResponseEntity.ok(matchService.listAvailableMatches(playerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDTO> getMatch(@RequestAttribute("playerId") String playerId, @PathVariable UUID id) {
        return ResponseEntity.ok(matchService.getMatch(playerId, id));
    }

    @PostMapping
    public ResponseEntity<MatchDTO> createMatch(@RequestAttribute("playerId") String playerId,
                                                @Valid @RequestBody CreateMatchDTO dto) {
        MatchDTO created = matchService.createMatch(playerId, dto);
        return ResponseEntity.created(URI.create("/api/matches/" + created.getId())).body(created);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<MatchDTO> joinMatch(@RequestAttribute("playerId") String playerId,
                                             @PathVariable UUID id,
                                             @Valid @RequestBody JoinMatchDTO dto) {
        MatchDTO updated = matchService.joinMatch(playerId, id, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveMatch(@RequestAttribute("playerId") String playerId,
                           @PathVariable UUID id) {
        matchService.leaveMatch(playerId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelMatch(@RequestAttribute("playerId") String playerId,
                           @PathVariable UUID id) {
        matchService.cancelMatch(playerId, id);
    }

    @GetMapping("/{id}/state")
    public ResponseEntity<MatchSnapshot> getMatchState(@RequestAttribute("playerId") String playerId,
                                                       @PathVariable UUID id) {
        MatchSnapshot snapshot = matchService.getMatchState(playerId, id);
        return ResponseEntity.ok(snapshot);
    }
}
