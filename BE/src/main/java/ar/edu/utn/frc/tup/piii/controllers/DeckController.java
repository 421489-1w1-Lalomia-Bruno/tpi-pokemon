package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final HttpServletRequest request;

    public DeckController(DeckService deckService, HttpServletRequest request) {
        this.deckService = deckService;
        this.request = request;
    }

    @GetMapping
    public ResponseEntity<List<DeckDTO>> listDecks() {
        String creatorId = (String) request.getAttribute("playerId");
        List<DeckDTO> decks = deckService.getDecksByCreator(creatorId);
        return ResponseEntity.ok(decks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckDTO> getDeckById(@PathVariable UUID id) {
        String creatorId = (String) request.getAttribute("playerId");
        DeckDTO deck = deckService.getDeckById(id, creatorId);
        return ResponseEntity.ok(deck);
    }

    @PostMapping
    public ResponseEntity<DeckDTO> createDeck(@Valid @RequestBody CreateDeckDTO dto) {
        String creatorId = (String) request.getAttribute("playerId");
        DeckDTO deck = deckService.createDeck(dto, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(deck);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeckDTO> updateDeck(@PathVariable UUID id,
                                              @Valid @RequestBody CreateDeckDTO dto) {
        String creatorId = (String) request.getAttribute("playerId");
        DeckDTO deck = deckService.updateDeck(id, dto, creatorId);
        return ResponseEntity.ok(deck);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(@PathVariable UUID id) {
        String creatorId = (String) request.getAttribute("playerId");
        deckService.deleteDeck(id, creatorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<DeckValidationResultDTO> validateDeck(@PathVariable UUID id) {
        String creatorId = (String) request.getAttribute("playerId");
        DeckValidationResultDTO result = deckService.validateDeckById(id, creatorId);
        return ResponseEntity.ok(result);
    }
}