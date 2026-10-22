package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.clients.dtos.AttackApiDTO;
import ar.edu.utn.frc.tup.piii.clients.dtos.AbilityApiDTO;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.rules.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.state.FinishedState;
import ar.edu.utn.frc.tup.piii.engine.state.MatchState;
import ar.edu.utn.frc.tup.piii.engine.state.WaitingState;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.enums.*;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameEngine {

    private static final Logger log = LoggerFactory.getLogger(GameEngine.class);

    private final Map<String, MatchState> matchStates = new ConcurrentHashMap<>();
    private final GameStatePersistenceService persistenceService;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;
    private final GameEventPublisher eventPublisher;
    private final RuleValidator ruleValidator;
    private final UserRepository userRepository;

    public GameEngine(GameStatePersistenceService persistenceService,
                      DeckRepository deckRepository,
                      CardRepository cardRepository,
                      MatchRepository matchRepository,
                      ObjectMapper objectMapper,
                      GameEventPublisher eventPublisher,
                      RuleValidator ruleValidator,
                      UserRepository userRepository) {
        this.persistenceService = persistenceService;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.matchRepository = matchRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.ruleValidator = ruleValidator;
        this.userRepository = userRepository;
    }

    public GameEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public RuleValidator getRuleValidator() {
        return ruleValidator;
    }

    public MatchEntity getMatchEntity(String matchId) {
        return matchRepository.findById(java.util.UUID.fromString(matchId)).orElse(null);
    }

    public void initializeMatch(String matchId) {
        WaitingState waitingState = new WaitingState();
        matchStates.put(matchId, waitingState);
        waitingState.enter(this, matchId);
    }

    public void transitionTo(String matchId, MatchState newState) {
        MatchState current = matchStates.get(matchId);
        if (current != null) {
            current.exit(this, matchId);
        }
        matchStates.put(matchId, newState);
        newState.enter(this, matchId);
    }

    public MatchState getCurrentState(String matchId) {
        return matchStates.get(matchId);
    }

    public Map<String, MatchState> getMatchStates() {
        return matchStates;
    }

    public void updateMatchStatus(String matchId, MatchStatus status) {
        MatchSnapshot snapshot = getSnapshot(matchId);
        if (snapshot != null) {
            snapshot.setStatus(status);
            saveSnapshot(matchId, snapshot);
        }
    }

    public void setMatchWinner(String matchId, String winnerId) {
        MatchEntity entity = getMatchEntity(matchId);
        if (entity != null) {
            entity.setWinnerId(winnerId);
            entity.setStatus(MatchStatus.FINISHED);
            matchRepository.save(entity);
        }
    }

    public List<GameCard> loadDeckCards(UUID deckId) {
        DeckEntity deck = deckRepository.findById(deckId).orElse(null);
        if (deck == null) {
            log.info("[GameEngine] loadDeckCards: deck not found " + deckId);
            return Collections.emptyList();
        }
        List<GameCard> cards = new ArrayList<>();
        for (DeckCardEntity entry : deck.getCards()) {
        CardEntity cardEntity = entry.getCard();
        for (int i = 0; i < entry.getQuantity(); i++) {
            GameCard card = toGameCard(cardEntity);
            card.setId(card.getId() + "-" + i);
            cards.add(card);
        }
    }
        return cards;
    }

    public GameCard toGameCard(CardEntity entity) {
        return switch (entity.getSupertype()) {
            case "Pokémon" -> toPokemonCard(entity);
            case "Energy" -> toEnergyCard(entity);
            case "Trainer" -> toTrainerCard(entity);
            default -> null;
        };
    }

    private PokemonCard toPokemonCard(CardEntity entity) {
        PokemonCard card = new PokemonCard();
        card.setId(entity.getId().toString());
        card.setName(entity.getName());
        card.setHp(entity.getHp());
        card.setStage(entity.getStage());
        card.setTypes(entity.getTypes());
        card.setEvolvesFrom(entity.getEvolvesFrom());
        card.setAttacks(parseAttacks(entity.getAttacksJson()));
        card.setAbilities(parseAbilities(entity.getAbilitiesJson()));
        card.setWeaknessType(entity.getWeaknessType());
        card.setResistanceType(entity.getResistanceType());
        card.setRetreatCost(entity.getRetreatCost());
        card.setImageUrl(entity.getImageUrl());
        return card;
    }

    private EnergyCard toEnergyCard(CardEntity entity) {
        EnergyCard card = new EnergyCard();
        card.setId(entity.getId().toString());
        card.setName(entity.getName());
        List<String> subtypes = entity.getSubtypes();
        boolean isSpecial = subtypes != null && subtypes.contains("Special");
        card.setSpecial(isSpecial);
        card.setImageUrl(entity.getImageUrl());
        if (entity.getTypes() != null && !entity.getTypes().isEmpty()) {
            EnergyType type = entity.getTypes().iterator().next();
            card.setEnergyType(type);
            if (isSpecial) {
                card.setProvidedEnergies(resolveSpecialEnergyProvided(card.getName(), type));
                card.setWildcard("Rainbow Energy".equals(card.getName()));
            } else {
                card.setProvidedEnergies(List.of(type));
            }
        } else {
            EnergyType type = deriveEnergyTypeFromName(entity.getName());
            card.setEnergyType(type);
            card.setProvidedEnergies(isSpecial ? resolveSpecialEnergyProvided(card.getName(), type) : (type != null ? List.of(type) : List.of()));
            card.setWildcard(isSpecial && "Rainbow Energy".equals(card.getName()));
        }
        return card;
    }

    private List<EnergyType> resolveSpecialEnergyProvided(String name, EnergyType defaultType) {
        return switch (name) {
            case "Double Colorless Energy" -> List.of(EnergyType.COLORLESS, EnergyType.COLORLESS);
            case "Rainbow Energy" -> List.of(EnergyType.COLORLESS);
            default -> defaultType != null ? List.of(defaultType) : List.of(EnergyType.COLORLESS);
        };
    }

    private EnergyType deriveEnergyTypeFromName(String name) {
        if (name == null) return EnergyType.COLORLESS;
        String upper = name.toUpperCase();
        for (EnergyType t : EnergyType.values()) {
            if (upper.contains(t.name())) return t;
        }
        return EnergyType.COLORLESS;
    }

    private TrainerCard toTrainerCard(CardEntity entity) {
        TrainerCard card = new TrainerCard();
        card.setId(entity.getId().toString());
        card.setName(entity.getName());
        card.setSubtype(deriveTrainerSubtype(entity.getSubtypes()));
        card.setEffect(entity.getRules());
        card.setImageUrl(entity.getImageUrl());
        return card;
    }

    private TrainerSubtype deriveTrainerSubtype(List<String> subtypes) {
        if (subtypes == null) return TrainerSubtype.ITEM;
        // Las "Pokémon Tool" llegan de la API como ["Item", "Pokémon Tool"]: TOOL debe
        // ganarle a ITEM, si no la herramienta nunca se adjunta. Además "Pokémon Tool"
        // no coincide literalmente con el enum TOOL, así que lo detectamos por substring.
        for (String s : subtypes) {
            String upper = s.toUpperCase().replace("É", "E").replace("'", "");
            if (upper.contains("TOOL")) return TrainerSubtype.TOOL;
            if (upper.contains("STADIUM")) return TrainerSubtype.STADIUM;
            if (upper.contains("SUPPORTER")) return TrainerSubtype.SUPPORTER;
            try {
                return TrainerSubtype.valueOf(upper);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return TrainerSubtype.ITEM;
    }

    private List<Attack> parseAttacks(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<AttackApiDTO> apiList = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AttackApiDTO.class));
            return apiList.stream().map(this::toAttack).toList();
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private Attack toAttack(AttackApiDTO api) {
        Attack attack = new Attack();
        attack.setName(api.getName());
        if (api.getCost() != null) {
            attack.setCost(api.getCost().stream()
                    .map(s -> {
                        try { return EnergyType.valueOf(s.toUpperCase()); }
                        catch (IllegalArgumentException e) { return null; }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList());
        }
        String damageStr = api.getDamage();
        String text = api.getText();
        if (damageStr == null || damageStr.isBlank()) {
            attack.setDamage(0);
            attack.setEffectText(text != null && !text.isBlank() ? text : null);
        } else if (damageStr.endsWith("+")) {
            try {
                attack.setDamage(Integer.parseInt(damageStr.replace("+", "")));
                attack.setEffectText(text != null && !text.isBlank() ? damageStr + " " + text : damageStr);
            } catch (NumberFormatException e) {
                attack.setDamage(0);
                attack.setEffectText(text != null ? damageStr + " " + text : damageStr);
            }
        } else if (damageStr.endsWith("\u00d7") || damageStr.endsWith("x")) {
            attack.setDamage(0);
            attack.setEffectText(text != null ? damageStr + " " + text : damageStr);
        } else {
            try {
                attack.setDamage(Integer.parseInt(damageStr));
                attack.setEffectText(text != null && !text.isBlank() ? text : null);
            } catch (NumberFormatException e) {
                attack.setDamage(0);
                attack.setEffectText(text != null ? damageStr + " " + text : damageStr);
            }
        }
        return attack;
    }

    private List<Ability> parseAbilities(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<AbilityApiDTO> apiList = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AbilityApiDTO.class));
            return apiList.stream().map(this::toAbility).toList();
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private Ability toAbility(AbilityApiDTO api) {
        Ability ability = new Ability();
        ability.setName(api.getName());
        ability.setEffectText(api.getText());
        String apiType = api.getType();
        if ("POKEMON_POWER".equals(apiType)) {
            ability.setType(ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_POWER);
        } else if ("POKE_BODY".equals(apiType)) {
            ability.setType(ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_BODY);
        } else if ("Ability".equals(apiType)) {
            String lower = api.getText() != null ? api.getText().toLowerCase() : "";
            ability.setType(lower.contains("during your turn")
                    ? ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_POWER
                    : ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_BODY);
        } else {
            ability.setType(ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_BODY);
        }
        return ability;
    }

    public MatchSnapshot processAction(String matchId, String playerId, String actionType, Map<String, Object> payload) {
        MatchState currentState = matchStates.get(matchId);
        if (currentState == null) {
            log.info("[GameEngine] processAction: no state for match " + matchId);
            return null;
        }
        GameActionType type = GameActionType.valueOf(actionType);
        GameActionMessage action = GameActionMessage.builder()
                .matchId(matchId)
                .type(type)
                .payload(payload)
                .build();
        log.info("[GameEngine] processAction: match=" + matchId + " player=" + playerId +
                " action=" + actionType + " currentState=" + currentState.getClass().getSimpleName());
        currentState.handleAction(this, action, playerId);
        MatchSnapshot snap = getSnapshot(matchId);
        log.info("[GameEngine] after handleAction: match=" + matchId +
                " snapshotStatus=" + (snap != null ? snap.getStatus() : "NULL") +
                " boardPresent=" + (snap != null && snap.getBoard() != null));
        return snap;
    }

    public void declareLoser(String matchId, String playerId, String reason) {
        MatchSnapshot snapshot = getSnapshot(matchId);
        if (snapshot == null) return;

        String winnerId = snapshot.getPlayer1Id().equals(playerId)
                ? snapshot.getPlayer2Id()
                : snapshot.getPlayer1Id();

        transitionTo(matchId, new FinishedState(winnerId));
    }

    public MatchSnapshot getSnapshot(String matchId) {
        return persistenceService.loadSnapshot(java.util.UUID.fromString(matchId));
    }

    public void saveSnapshot(String matchId, MatchSnapshot snapshot) {
        persistenceService.saveSnapshot(snapshot);
    }

    public void updatePlayerStats(String matchId, String winnerId) {
        MatchSnapshot snapshot = getSnapshot(matchId);
        if (snapshot == null) return;

        String loserId = winnerId.equals(snapshot.getPlayer1Id())
                ? snapshot.getPlayer2Id()
                : snapshot.getPlayer1Id();

        try {
            UUID winnerUuid = UUID.fromString(winnerId);
            userRepository.findById(winnerUuid).ifPresent(user -> {
                user.setWins(user.getWins() + 1);
                userRepository.save(user);
            });

            if (loserId != null) {
                UUID loserUuid = UUID.fromString(loserId);
                userRepository.findById(loserUuid).ifPresent(user -> {
                    user.setLosses(user.getLosses() + 1);
                    userRepository.save(user);
                });
            }
        } catch (Exception e) {
            log.error("[GameEngine] Failed to update player stats for match " + matchId + ": " + e.getMessage());
        }
    }
}
