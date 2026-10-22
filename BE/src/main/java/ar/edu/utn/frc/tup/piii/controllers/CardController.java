package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncResponseDTO;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.services.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public ResponseEntity<Page<CardDTO>> listCards(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) EnergyType type,
            @RequestParam(required = false) PokemonStage stage,
            @RequestParam(required = false) String supertype,
            @RequestParam(required = false) String setId,
            @RequestParam(required = false) String name) {
        CardFilterDTO filter = new CardFilterDTO();
        filter.setType(type);
        filter.setStage(stage);
        filter.setSupertype(supertype);
        filter.setSetId(setId);
        filter.setName(name);
        return ResponseEntity.ok(cardService.listCards(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardDTO> getCardById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CardDTO>> searchCards(@RequestParam String name) {
        return ResponseEntity.ok(cardService.searchCards(name));
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncResponseDTO> syncCards(@Valid @RequestBody SyncRequestDTO request) {
        SyncResponseDTO response = cardService.syncCards(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
