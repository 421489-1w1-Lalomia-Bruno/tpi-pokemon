package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;

import java.util.List;
import java.util.UUID;

public interface DeckService {

    DeckValidationResultDTO validateDeck(CreateDeckDTO dto);

    DeckDTO createDeck(CreateDeckDTO dto, String creatorId);

    DeckDTO getDeckById(UUID id, String creatorId);

    List<DeckDTO> getDecksByCreator(String creatorId);

    DeckDTO updateDeck(UUID id, CreateDeckDTO dto, String creatorId);

    void deleteDeck(UUID id, String creatorId);

    DeckValidationResultDTO validateDeckById(UUID id, String creatorId);
}
