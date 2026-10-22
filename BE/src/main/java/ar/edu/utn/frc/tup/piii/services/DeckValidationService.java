package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;

@FunctionalInterface
public interface DeckValidationService {

    DeckValidationResultDTO validateDeck(CreateDeckDTO dto);
}
