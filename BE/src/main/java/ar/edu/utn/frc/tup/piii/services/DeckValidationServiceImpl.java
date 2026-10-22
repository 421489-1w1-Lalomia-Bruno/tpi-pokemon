package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardEntryDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DeckValidationServiceImpl implements DeckValidationService {

    private static final int DECK_SIZE_REQUIRED = 60;
    private static final int MAX_COPIES_PER_CARD = 4;
    private static final int MIN_BASIC_POKEMON = 1;

    private static final Logger log = LoggerFactory.getLogger(DeckValidationServiceImpl.class);

    private final CardRepository cardRepository;

    public DeckValidationServiceImpl(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public DeckValidationResultDTO validateDeck(CreateDeckDTO dto) {
        List<String> errors = new ArrayList<>();
        int totalCards = 0;
        int basicPokemonCount = 0;
        Map<String, Integer> cardNameCounts = new HashMap<>();

        Set<UUID> uniqueCardIds = new HashSet<>();
        boolean hasInvalidIds = false;
        for (DeckCardEntryDTO entry : dto.getCards()) {
            UUID id = parseCardId(entry.getCardId());
            if (id == null) {
                hasInvalidIds = true;
            } else {
                uniqueCardIds.add(id);
            }
        }

        if (hasInvalidIds) {
            errors.add("One or more card IDs have an invalid format");
        }

        List<CardEntity> cards = cardRepository.findAllById(uniqueCardIds);
        if (cards.size() != uniqueCardIds.size()) {
            errors.add("One or more card IDs in the deck do not exist");
        }

        if (hasInvalidIds || cards.size() != uniqueCardIds.size()) {
            return DeckValidationResultDTO.builder()
                    .isValid(false)
                    .errors(errors)
                    .totalCards(totalCards)
                    .basicPokemonCount(basicPokemonCount)
                    .cardNameCounts(cardNameCounts)
                    .build();
        }

        Map<UUID, CardEntity> cardMap = new HashMap<>();
        for (CardEntity card : cards) {
            cardMap.put(card.getId(), card);
        }

        for (DeckCardEntryDTO entry : dto.getCards()) {
            UUID cardId = parseCardId(entry.getCardId());
            CardEntity card = cardMap.get(cardId);
            int quantity = entry.getQuantity();

            totalCards += quantity;

            cardNameCounts.merge(card.getName(), quantity, Integer::sum);

            if (isBasicPokemon(card)) {
                basicPokemonCount += quantity;
            }

        }

        if (totalCards != DECK_SIZE_REQUIRED) {
            errors.add("Deck must have exactly " + DECK_SIZE_REQUIRED + " cards, but has " + totalCards);
        }

        for (Map.Entry<String, Integer> entry : cardNameCounts.entrySet()) {
            String cardName = entry.getKey();
            int count = entry.getValue();

            CardEntity sampleCard = findFirstCardWithName(cardName, cardMap);
            if (isBasicEnergy(sampleCard)) {
                continue;
            }

            if (count > MAX_COPIES_PER_CARD) {
                errors.add("Card '" + cardName + "' has " + count + " copies, but maximum is " + MAX_COPIES_PER_CARD);
            }
        }

        if (basicPokemonCount < MIN_BASIC_POKEMON) {
            errors.add("Deck must contain at least " + MIN_BASIC_POKEMON + " Basic Pokémon");
        }

        validateEvolutionChain(cardMap, cardNameCounts, errors);

        boolean isValid = errors.isEmpty();

        log.info("Deck validation result: valid={}, totalCards={}, basicPokemon={}, errors={}",
                isValid, totalCards, basicPokemonCount, errors.size());

        return DeckValidationResultDTO.builder()
                .isValid(isValid)
                .errors(errors)
                .totalCards(totalCards)
                .basicPokemonCount(basicPokemonCount)
                .cardNameCounts(cardNameCounts)
                .build();
    }

    private UUID parseCardId(String cardId) {
        try {
            return UUID.fromString(cardId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void validateEvolutionChain(Map<UUID, CardEntity> cardMap, Map<String, Integer> cardNameCounts, List<String> errors) {
        for (CardEntity card : cardMap.values()) {
            if (card.getEvolvesFrom() != null && !card.getEvolvesFrom().isBlank()) {
                String preEvolutionName = card.getEvolvesFrom();
                if (!cardNameCounts.containsKey(preEvolutionName)) {
                    errors.add("Card '" + card.getName() + "' evolves from '" + preEvolutionName
                            + "', but '" + preEvolutionName + "' is not in the deck");
                }
            }
        }
    }

    private boolean isBasicPokemon(CardEntity card) {
        return "Pokémon".equals(card.getSupertype())
                && (card.getStage() == PokemonStage.BASIC
                    || (card.getStage() == PokemonStage.EX && card.getEvolvesFrom() == null));
    }

    private boolean isBasicEnergy(CardEntity card) {
        return "Energy".equals(card.getSupertype())
                && card.getSubtypes() != null
                && card.getSubtypes().contains("Basic");
    }

    private CardEntity findFirstCardWithName(String name, Map<UUID, CardEntity> cardMap) {
        return cardMap.values().stream()
                .filter(card -> card.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Card not found in map: " + name));
    }
}
