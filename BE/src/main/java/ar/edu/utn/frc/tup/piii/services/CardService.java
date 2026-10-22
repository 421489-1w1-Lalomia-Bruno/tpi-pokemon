package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CardService {
    SyncResponseDTO syncCards(SyncRequestDTO request);
    Page<CardDTO> listCards(CardFilterDTO filter, Pageable pageable);
    CardDTO getCardById(UUID id);
    List<CardDTO> searchCards(String name);
}
