package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.combat.AttackFlipResolver;
import ar.edu.utn.frc.tup.piii.engine.combat.AttackResolver;
import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectContext;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.effects.trainer.EvolveFromDeckEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.engine.effects.trainer.TrainerEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.events.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.rules.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.rules.VictoryConditionChecker;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.*;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveState implements MatchState {

    private static final Logger log = LoggerFactory.getLogger(ActiveState.class);

    private final Map<String, TurnManager> turnManagers = new ConcurrentHashMap<>();
    private final VictoryConditionChecker victoryChecker = new VictoryConditionChecker();
    private final TrainerEffectResolver trainerEffectResolver = new TrainerEffectResolver();
    private final AbilityEffectResolver abilityEffectResolver = new AbilityEffectResolver();

    @Override
    public GamePhase getPhase() {
        return GamePhase.ACTIVE;
    }

    private static final long INITIAL_TIME_MS = 40 * 60 * 1000L; // 40 min por jugador

    @Override
    public void enter(GameEngine engine, String matchId) {
        MatchSnapshot snapshot = engine.getSnapshot(matchId);
        if (snapshot == null || snapshot.getBoard() == null) {
            return;
        }

        TurnContext turnCtx = snapshot.getTurnContext();
        if (turnCtx == null) {
            turnCtx = new TurnContext();
            turnCtx.setPhase(TurnPhase.DRAW);
            turnCtx.setFirstTurn(true);
            snapshot.setTurnContext(turnCtx);
        }

        if (snapshot.getPlayer1TimeRemainingMs() == 0 && snapshot.getPlayer2TimeRemainingMs() == 0) {
            snapshot.setPlayer1TimeRemainingMs(INITIAL_TIME_MS);
            snapshot.setPlayer2TimeRemainingMs(INITIAL_TIME_MS);
        }
        turnCtx.setTurnStartTimeMs(System.currentTimeMillis());

        TurnManager turnManager = new TurnManager(
                matchId,
                snapshot.getPlayer1Id(),
                snapshot.getPlayer2Id(),
                turnCtx,
                new StatusEffectManager(),
                engine.getEventPublisher()
        );
        turnManagers.put(matchId, turnManager);

        GameBoard board = snapshot.getBoard();
        turnManager.executeDrawPhase(engine, board);

        // Si durante el sorteo el mazo se agotó, el estado ya es FINISHED
        if (snapshot.getStatus() == MatchStatus.FINISHED) {
            engine.saveSnapshot(matchId, snapshot);
            return;
        }

        snapshot.setStatus(MatchStatus.ACTIVE);
        engine.saveSnapshot(matchId, snapshot);

        engine.getEventPublisher().publish(GameEventType.PHASE_CHANGED, matchId,
                turnManager.getCurrentPlayerId(), "Game active - " + turnManager.getPhase());
    }

    @Override
    public void exit(GameEngine engine, String matchId) {
        turnManagers.remove(matchId);
    }

    @Override
    public void handleAction(GameEngine engine, GameActionMessage action, String playerId) {
        String matchId = action.getMatchId();
        MatchSnapshot snapshot = engine.getSnapshot(matchId);
        if (snapshot == null) {
            throw new InvalidActionException("Match not found");
        }

        engine.getRuleValidator().validateAction(action, playerId, snapshot);

        TurnManager turnManager = turnManagers.get(matchId);
        if (turnManager == null) {
            throw new InvalidActionException("Active state not initialized for this match");
        }

        snapshot.setTurnContext(turnManager.getContext());
        snapshot.setCurrentPlayerId(turnManager.getCurrentPlayerId());

        GameBoard board = snapshot.getBoard();
        GameActionType type = action.getType();

        switch (type) {
            case SURRENDER -> handleSurrender(engine, matchId, playerId);
            case ATTACK -> handleAttack(engine, matchId, snapshot, turnManager, board, action);
            case PASS_TURN -> handlePassTurn(engine, matchId, snapshot, turnManager, board);
            case SET_ACTIVE -> handleSetActive(engine, matchId, snapshot, turnManager, action, playerId);
            case ATTACH_ENERGY -> handleAttachEnergy(engine, matchId, snapshot, turnManager, action, playerId);
            case PLAY_TRAINER -> handlePlayTrainer(engine, matchId, snapshot, turnManager, action, playerId);
            case EVOLVE -> handleEvolve(engine, matchId, snapshot, turnManager, action, playerId);
            case RETREAT -> handleRetreat(engine, matchId, snapshot, turnManager, action, playerId);
            case USE_ABILITY -> handleUseAbility(engine, matchId, snapshot, action, playerId);
            case PLACE_POKEMON -> handlePlacePokemon(engine, matchId, snapshot, action, playerId);
            case TAKE_PRIZE -> handleTakePrize(engine, matchId, snapshot, turnManager, board, action, playerId);
            case RESOLVE_REVEAL -> handleResolveReveal(engine, matchId, snapshot, action, playerId);
            case SELECT_TARGET -> handleSelectTarget(engine, matchId, snapshot, turnManager, board, action, playerId);
            default -> throw new InvalidActionException("Unknown action: " + type);
        }

        snapshot.setCurrentPlayerId(turnManager.getCurrentPlayerId());
        snapshot.setTurnContext(turnManager.getContext());

        if (engine.getCurrentState(matchId) instanceof ActiveState) {
            String winnerId = victoryChecker.checkVictory(snapshot);
            if (winnerId != null) {
                engine.transitionTo(matchId, new FinishedState(winnerId));
                return;
            }
            engine.saveSnapshot(matchId, snapshot);
        }
    }

    private void handleSurrender(GameEngine engine, String matchId, String playerId) {
        MatchSnapshot snapshot = engine.getSnapshot(matchId);
        if (snapshot != null) {
            engine.saveSnapshot(matchId, snapshot);
        }
        engine.declareLoser(matchId, playerId, "Surrender");
    }

    /**
     * Resuelve un "reveal" pendiente (p. ej. Great Ball): el jugador eligió una
     * carta (cardId) que va a su mano; el resto vuelve al mazo barajado. Si no
     * eligió nada (o no había Pokémon), todo vuelve al mazo.
     */
    private void handleResolveReveal(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                     GameActionMessage action, String playerId) {
        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }
        if (!board.isHasPendingReveal()) {
            throw new InvalidActionException("No pending reveal to resolve");
        }

        List<GameCard> revealed = board.getRevealedCards();
        if (revealed == null || revealed.isEmpty()) {
            board.setHasPendingReveal(false);
            board.setRevealedCards(null);
            return;
        }

        boolean isHandDisruption = !revealed.isEmpty()
            && revealed.get(0).getId().startsWith("opp-hand-");

        log.info("[Astonish] handleResolveReveal: isHandDisruption={}, firstCardId={}, cardIdFromPayload={}",
                isHandDisruption,
                revealed.get(0).getId(),
                action.getPayload() != null ? action.getPayload().get("cardId") : "null");

        if (isHandDisruption) {
            resolveHandDisruptionReveal(engine, matchId, snapshot, action, playerId, board, revealed);
            return;
        }

        Map<String, Object> payload = action.getPayload();
        String cardId = payload != null ? (String) payload.get("cardId") : null;

        if (cardId == null) {
            board.getDeck().addAll(revealed);
            java.util.Collections.shuffle(board.getDeck());
            board.setHasPendingReveal(false);
            board.setRevealedCards(null);
            engine.getEventPublisher().publish(GameEventType.PHASE_CHANGED, matchId, playerId,
                    "No Pokemon chosen, shuffled cards back into deck");
            return;
        }

        GameCard chosen = null;
        for (GameCard card : revealed) {
            if (card.getId().equals(cardId)) {
                chosen = card;
                break;
            }
        }
        if (chosen == null) {
            throw new InvalidActionException("Chosen card not found in revealed cards");
        }
        if (!(chosen instanceof PokemonCard)) {
            throw new InvalidActionException("You must choose a Pokemon card");
        }

        revealed.remove(chosen);
        board.getHand().add(chosen);

        board.getDeck().addAll(revealed);
        java.util.Collections.shuffle(board.getDeck());

        board.setHasPendingReveal(false);
        board.setRevealedCards(null);

        engine.getEventPublisher().publish(GameEventType.CARD_DRAWN, matchId, playerId,
                "Great Ball: " + chosen.getName() + " added to hand");
    }

    private void resolveHandDisruptionReveal(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                              GameActionMessage action, String attackerId,
                                              PlayerBoard attackerBoard, List<GameCard> faceDownCards) {
        Map<String, Object> payload = action.getPayload();
        String cardId = payload != null ? (String) payload.get("cardId") : null;
        if (cardId == null) {
            throw new InvalidActionException("You must choose a card");
        }

        int index = -1;
        for (int i = 0; i < faceDownCards.size(); i++) {
            if (faceDownCards.get(i).getId().equals(cardId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new InvalidActionException("Chosen card not found");
        }

        PlayerBoard opponentBoard = getOpponentBoard(snapshot, attackerId);
        if (opponentBoard == null) {
            throw new InvalidActionException("Opponent board not found");
        }

        List<GameCard> opponentHand = opponentBoard.getHand();
        if (index >= opponentHand.size()) {
            throw new InvalidActionException("Invalid card index");
        }

        GameCard chosenCard = opponentHand.remove(index);
        opponentBoard.getDeck().add(chosenCard);
        java.util.Collections.shuffle(opponentBoard.getDeck());

        log.info("[Astonish] Card removed: {} (id={}). Opponent hand size now: {}",
                chosenCard.getName(), chosenCard.getId(), opponentHand.size());

        attackerBoard.setRevealedCards(null);
        attackerBoard.setHasPendingReveal(false);

        GameEvent revealEvent = new GameEvent(GameEventType.REVEAL_CARDS, matchId, attackerId,
                "Opponent's " + chosenCard.getName() + " was shuffled into their deck!",
                LocalDateTime.now(), null, chosenCard.getName(), null);
        revealEvent.setCard(chosenCard);
        engine.getEventPublisher().publishEvent(revealEvent);

        TurnManager tm = turnManagers.get(matchId);
        GameBoard gb = snapshot.getBoard();
        if (tm != null && gb != null) {
            continueTurnAfter(engine, matchId, snapshot, tm, gb);
        }
    }

    private void handleAttack(GameEngine engine, String matchId, MatchSnapshot snapshot,
                               TurnManager turnManager, GameBoard board, GameActionMessage action) {
        turnManager.executeAttackPhase(engine);

        String currentPlayerId = turnManager.getCurrentPlayerId();
        PlayerBoard attackerBoard = getPlayerBoard(snapshot, currentPlayerId);
        if (attackerBoard == null || attackerBoard.getActivePokemon() == null) {
            throw new InvalidActionException("No active Pokemon to attack with");
        }

        PokemonInPlay attacker = attackerBoard.getActivePokemon();
        if (attacker.getCard().getAttacks() == null || attacker.getCard().getAttacks().isEmpty()) {
            throw new InvalidActionException("Active Pokemon has no attacks");
        }

        List<Attack> attacks = attacker.getCard().getAttacks();
        int attackIdx = 0;
        Map<String, Object> atkPayload = action.getPayload();
        if (atkPayload != null && atkPayload.get("attackIndex") instanceof Number n) {
            attackIdx = Math.max(0, Math.min(n.intValue(), attacks.size() - 1));
        }
        Attack selectedAttack = attacks.get(attackIdx);

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(selectedAttack);

        int baseDamage = selectedAttack.getDamage();
        String effectText = selectedAttack.getEffectText();
        if (effectText != null && AttackFlipResolver.isFlipAttack(effectText)) {
            AttackFlipResolver.FlipResult flipResult = AttackFlipResolver.resolveWithEvents(
                    effectText, engine.getEventPublisher(), matchId, currentPlayerId,
                    attacker.getCard().getName() + " used " + selectedAttack.getName());
            baseDamage = flipResult.getDamage();
            ctx.setFlipResolvedByAttack(true);
        }
        ctx.setBoard(board);
        ctx.setCurrentPlayerId(currentPlayerId);
        ctx.setMatchId(matchId);
        ctx.setEventPublisher(engine.getEventPublisher());

        List<SpecialCondition> conditions;
        if (effectText != null && AttackFlipResolver.isConditionalFlipAttack(effectText)) {
            ctx.setNeedsConditionalFlip(true);
            conditions = new ArrayList<>();
        } else {
            conditions = AttackFlipResolver.parseSpecialConditions(effectText);
        }
        ctx.setBaseDamage(baseDamage);
        ctx.setEffectsToApply(conditions);

        AttackResolver resolver = new AttackResolver();
        resolver.resolveAttack(ctx);

        GameCard revealedCard = ctx.getRevealedCard();
        if (revealedCard != null) {
            GameEvent revealEvent = new GameEvent(GameEventType.REVEAL_CARDS, matchId, currentPlayerId,
                    attacker.getCard().getName() + " used " + selectedAttack.getName()
                    + "! Opponent's " + revealedCard.getName() + " was shuffled into their deck.",
                    LocalDateTime.now(), attacker.getCard().getName(), revealedCard.getName(), null);
            revealEvent.setCard(revealedCard);
            engine.getEventPublisher().publishEvent(revealEvent);
        }

        if (ctx.isAttackSuccessful()) {
            String defenderName = ctx.getDefender() != null ? ctx.getDefender().getCard().getName() : "unknown";
            Integer finalDmg = ctx.getFinalDamage() != null ? ctx.getFinalDamage() : 0;

            String attackDescription;
            if (finalDmg == 0 && ctx.getVariableDamageZeroDescription() != null) {
                attackDescription = ctx.getVariableDamageZeroDescription();
            } else {
                attackDescription = attacker.getCard().getName() + " used " + selectedAttack.getName();
            }

            engine.getEventPublisher().publish(GameEventType.ATTACK_EXECUTED, matchId, currentPlayerId,
                    attackDescription,
                    attacker.getCard().getName(), defenderName, finalDmg);

            PokemonInPlay defender = ctx.getDefender();
            boolean defenderKO = defender != null && defender.isKnockedOut();
            if (defenderKO) {
                PlayerBoard defenderBoard = getOpponentBoard(snapshot, currentPlayerId);
                handleKnockout(engine, matchId, snapshot, defender, defenderBoard, currentPlayerId);
            }

            PokemonInPlay attackerPoke = ctx.getAttacker();
            boolean attackerKO = attackerPoke != null && attackerPoke.isKnockedOut();
            if (attackerKO) {
                PlayerBoard atkBoard = getPlayerBoard(snapshot, currentPlayerId);
                String opponentId = snapshot.getPlayer1Id().equals(currentPlayerId)
                        ? snapshot.getPlayer2Id() : snapshot.getPlayer1Id();
                handleKnockout(engine, matchId, snapshot, attackerPoke, atkBoard, opponentId);
            }
        } else {
            String reason = ctx.getFailureReason() != null ? ctx.getFailureReason() : "El ataque falló";
            engine.getEventPublisher().publish(GameEventType.ATTACK_FAILED, matchId, currentPlayerId, reason);

            PokemonInPlay attackerPoke = ctx.getAttacker();
            if (attackerPoke != null && attackerPoke.isKnockedOut()) {
                PlayerBoard atkBoard = getPlayerBoard(snapshot, currentPlayerId);
                String opponentId = snapshot.getPlayer1Id().equals(currentPlayerId)
                        ? snapshot.getPlayer2Id() : snapshot.getPlayer1Id();
                handleKnockout(engine, matchId, snapshot, attackerPoke, atkBoard, opponentId);

                String winnerId = victoryChecker.checkVictory(snapshot);
                if (winnerId != null) {
                    engine.transitionTo(matchId, new FinishedState(winnerId));
                    return;
                }

                engine.saveSnapshot(matchId, snapshot);
                return;
            }

            // Confusion failure without KO → end turn immediately
            if (attackerPoke != null
                    && attackerPoke.getSpecialConditions().contains(SpecialCondition.CONFUSED)) {
                continueTurnAfter(engine, matchId, snapshot, turnManager, board);
                return;
            }

            // Failed attack with hand-disruption: set pending reveal
            List<GameCard> failedFaceDown = ctx.getFaceDownCardsForReveal();
            if (failedFaceDown != null && !failedFaceDown.isEmpty()) {
                PlayerBoard failBoard = getPlayerBoard(snapshot, currentPlayerId);
                if (failBoard != null) {
                    failBoard.setRevealedCards(new ArrayList<>(failedFaceDown));
                    failBoard.setHasPendingReveal(true);
                }
                engine.saveSnapshot(matchId, snapshot);
                return;
            }

            turnManager.getContext().setPhase(TurnPhase.ACTIONS);
            engine.saveSnapshot(matchId, snapshot);
            return;
        }

        // Process hand-disruption face-down cards AFTER KO processing
        List<GameCard> faceDownCards = ctx.getFaceDownCardsForReveal();
        if (faceDownCards != null && !faceDownCards.isEmpty()) {
            PlayerBoard atkBoard = getPlayerBoard(snapshot, currentPlayerId);
            if (atkBoard != null) {
                atkBoard.setRevealedCards(new ArrayList<>(faceDownCards));
                atkBoard.setHasPendingReveal(true);
            }
            engine.saveSnapshot(matchId, snapshot);
            return;
        }

        PlayerBoard atkBoard = getPlayerBoard(snapshot, currentPlayerId);
        if (atkBoard != null && atkBoard.getPendingPrizeCount() > 0) {
            engine.saveSnapshot(matchId, snapshot);
            return;
        }

        PlayerBoard oppBoard = getOpponentBoard(snapshot, currentPlayerId);
        if (oppBoard != null && oppBoard.getPendingPrizeCount() > 0) {
            engine.saveSnapshot(matchId, snapshot);
            return;
        }

        if (ctx.isPausedForTargetSelection()) {
            if (atkBoard != null) {
                atkBoard.setPendingMultiCoinTargets(ctx.getPendingMultiCoinTargets());
                atkBoard.setPendingMultiCoinEnergyType(ctx.getPendingMultiCoinEnergyType());
            }
            engine.saveSnapshot(matchId, snapshot);
            return;
        }

        continueTurnAfter(engine, matchId, snapshot, turnManager, board);
    }

    private void continueTurnAfter(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                    TurnManager turnManager, GameBoard board) {
        turnManager.executeBetweenTurns(engine, board);

        PlayerBoard curBoard = getPlayerBoard(snapshot, turnManager.getCurrentPlayerId());
        boolean curNeedsPromotion = curBoard != null
                && curBoard.getActivePokemon() == null
                && !curBoard.getBench().isEmpty();

        PlayerBoard oppBoard = getOpponentBoard(snapshot, turnManager.getCurrentPlayerId());
        boolean oppNeedsPromotion = oppBoard != null
                && oppBoard.getActivePokemon() == null
                && !oppBoard.getBench().isEmpty();

        if (curNeedsPromotion || oppNeedsPromotion) {
            turnManager.setPhase(TurnPhase.BETWEEN_TURNS);
            return;
        }

        if (!isStillActive(engine, matchId)) return;

        turnManager.nextTurn(snapshot);

        turnManager.executeStartOfTurnEffects(engine, board);
        if (handleBetweenTurnsKO(engine, matchId, snapshot, board, turnManager)) {
            return;
        }

        turnManager.executeDrawPhase(engine, board);
    }

    private void handlePassTurn(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                 TurnManager turnManager, GameBoard board) {
        turnManager.executeBetweenTurns(engine, board);

        if (!isStillActive(engine, matchId)) return;

        turnManager.nextTurn(snapshot);

        turnManager.executeStartOfTurnEffects(engine, board);
        if (handleBetweenTurnsKO(engine, matchId, snapshot, board, turnManager)) {
            return;
        }

        turnManager.executeDrawPhase(engine, board);
    }

    private void handleSetActive(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                  TurnManager turnManager, GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String cardId = extractCardId(payload);

        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }

        PokemonInPlay toPromote = null;
        for (PokemonInPlay bench : board.getBench()) {
            if (bench.getCard().getId().equals(cardId)) {
                toPromote = bench;
                break;
            }
        }

        if (toPromote == null) {
            throw new InvalidActionException("Pokemon not found in bench: " + cardId);
        }

        board.getBench().remove(toPromote);
        if (board.getActivePokemon() != null) {
            board.getActivePokemon().setActive(false);
            board.getBench().add(board.getActivePokemon());
        }
        board.setActivePokemon(toPromote);
        toPromote.setActive(true);

        if (turnManager.getPhase() == TurnPhase.BETWEEN_TURNS) {
            String otherPlayerId = snapshot.getPlayer1Id().equals(playerId)
                    ? snapshot.getPlayer2Id() : snapshot.getPlayer1Id();
            PlayerBoard otherBoard = getPlayerBoard(snapshot, otherPlayerId);
            boolean otherNeedsPromotion = otherBoard != null
                    && otherBoard.getActivePokemon() == null
                    && !otherBoard.getBench().isEmpty();

            if (otherNeedsPromotion) {
                turnManager.getContext().setPhase(TurnPhase.BETWEEN_TURNS);
                return;
            }

            // Don't advance turn if prizes are still pending
            PlayerBoard curBoard = getPlayerBoard(snapshot, playerId);
            PlayerBoard oppBoard = getOpponentBoard(snapshot, playerId);
            if ((curBoard != null && curBoard.getPendingPrizeCount() > 0)
                    || (oppBoard != null && oppBoard.getPendingPrizeCount() > 0)) {
                turnManager.getContext().setPhase(TurnPhase.BETWEEN_TURNS);
                engine.saveSnapshot(matchId, snapshot);
                return;
            }

            // Only advance the turn when the OPPONENT promotes after an attack KO during my
            // turn. When the current player promotes their own active that was KO'd at the
            // start of their turn (poison/burn), the turn was already advanced before the KO,
            // so they keep playing — don't advance again or their turn would be skipped.
            if (!playerId.equals(turnManager.getCurrentPlayerId())) {
                turnManager.nextTurn(snapshot);
            }

            turnManager.executeStartOfTurnEffects(engine, snapshot.getBoard());
            if (handleBetweenTurnsKO(engine, matchId, snapshot, snapshot.getBoard(), turnManager)) {
                return;
            }

            turnManager.executeDrawPhase(engine, snapshot.getBoard());
        } else {
            turnManager.setPhase(TurnPhase.ACTIONS);
        }

        engine.getEventPublisher().publish(GameEventType.PHASE_CHANGED, matchId, playerId,
                "Promoted to active: " + toPromote.getCard().getName());
    }

    private void handleAttachEnergy(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                    TurnManager turnManager, GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String cardId = extractCardId(payload);
        String targetId = extractTargetId(payload);

        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }

        GameCard card = findCardInHand(board, cardId);
        if (card == null) {
            throw new InvalidActionException("Energy card not found in hand");
        }
        if (!(card instanceof EnergyCard energyCard)) {
            throw new InvalidActionException("Card is not an Energy card");
        }

        PokemonInPlay target = findPokemonOnBoard(board, targetId);
        if (target == null) {
            throw new InvalidActionException("Target Pokemon not found on your board");
        }

        board.getHand().remove(card);
        target.getAttachedEnergy().add(energyCard);
        turnManager.getContext().setHasAttachedEnergy(true);

        engine.getEventPublisher().publish(GameEventType.ENERGY_ATTACHED, matchId, playerId,
                "Energy attached to " + target.getCard().getName());
    }

    private void checkOpponentPassiveTrainerBlock(MatchSnapshot snapshot, String playerId, TrainerCard trainerCard) {
        PlayerBoard opponentBoard = getOpponentBoard(snapshot, playerId);
        if (opponentBoard == null) return;
        PokemonInPlay opponentActive = opponentBoard.getActivePokemon();
        if (opponentActive == null || opponentActive.getCard().getAbilities() == null) return;
        GameBoard gameBoard = snapshot.getBoard();
        PlayerBoard playerBoard = getPlayerBoard(snapshot, playerId);
        String subtype = trainerCard.getSubtype() != null ? trainerCard.getSubtype().name() : "";
        for (Ability ability : opponentActive.getCard().getAbilities()) {
            if (ability.getType() != AbilityType.POKE_BODY) continue;
            AbilityEffectContext ctx = new AbilityEffectContext(
                    null, playerId, opponentActive, gameBoard, opponentBoard, playerBoard, null, TurnPhase.ACTIONS);
            for (AbilityEffect effect : abilityEffectResolver.resolve(ability)) {
                if (effect.blocksTrainerPlay(subtype, ctx)) {
                    throw new InvalidActionException(
                            opponentActive.getCard().getName() + " impide jugar cartas de tipo " + subtype);
                }
            }
        }
    }

    private void handlePlayTrainer(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                   TurnManager turnManager, GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String cardId = extractCardId(payload);

        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }

        GameCard card = findCardInHand(board, cardId);
        if (card == null) {
            throw new InvalidActionException("Trainer card not found in hand");
        }
        if (!(card instanceof TrainerCard trainerCard)) {
            throw new InvalidActionException("Card is not a Trainer card");
        }

        checkOpponentPassiveTrainerBlock(snapshot, playerId, trainerCard);

        TurnContext turnCtx = turnManager.getContext();

        switch (trainerCard.getSubtype()) {
            case SUPPORTER -> {
                if (turnCtx.isHasPlayedSupporter()) {
                    throw new InvalidActionException("Already played a Supporter this turn");
                }
                turnCtx.setHasPlayedSupporter(true);
                if (!applyTrainerEffects(trainerCard, engine, matchId, snapshot, action, playerId, turnCtx)) {
                    turnCtx.setHasPlayedSupporter(false);
                    return;
                }
                board.getHand().remove(card);
                board.getDiscardPile().add(card);
            }
            case STADIUM -> {
                if (turnCtx.isHasPlayedStadium()) {
                    throw new InvalidActionException("Already played a Stadium this turn");
                }
                GameBoard gb = snapshot.getBoard();
                TrainerCard oldStadium = gb.getSharedStadium();
                if (oldStadium != null) {
                    board.getDiscardPile().add(oldStadium);
                }
                gb.setSharedStadium(trainerCard);
                turnCtx.setHasPlayedStadium(true);
                board.getHand().remove(card);
            }
            case TOOL -> {
                String targetId = extractTargetId(payload);
                PokemonInPlay target = findPokemonOnBoard(board, targetId);
                if (target == null) {
                    throw new InvalidActionException("Target Pokemon not found");
                }
                if (target.getTool() != null) {
                    board.getDiscardPile().add(target.getTool());
                }
                target.setTool(trainerCard);
                board.getHand().remove(card);
            }
            case ITEM -> {
                if (!applyTrainerEffects(trainerCard, engine, matchId, snapshot, action, playerId, turnCtx)) {
                    return;
                }
                board.getHand().remove(card);
                board.getDiscardPile().add(card);
            }
            default -> {}
        }

        engine.getEventPublisher().publish(GameEventType.TRAINER_PLAYED, matchId, playerId,
                "Played trainer: " + trainerCard.getName());
    }

    private boolean applyTrainerEffects(TrainerCard trainerCard, GameEngine engine, String matchId,
                                        MatchSnapshot snapshot, GameActionMessage action, String playerId,
                                        TurnContext turnCtx) {
        TrainerEffectContext ctx = new TrainerEffectContext(
                snapshot.getBoard(),
                playerId,
                matchId,
                trainerCard,
                extractTargetId(action.getPayload()),
                engine.getEventPublisher()
        );

        List<TrainerEffect> effects = trainerEffectResolver.resolve(trainerCard);
        if (effects.isEmpty()) return true;

        for (TrainerEffect effect : effects) {
            if (effect instanceof EvolveFromDeckEffect && turnCtx.isFirstTurn()) {
                if (engine.getEventPublisher() != null) {
                    engine.getEventPublisher().publish(
                            GameEventType.NOTIFICATION, matchId, playerId,
                            trainerCard.getName() + ": no se puede usar en el primer turno");
                }
                return false;
            }
            if (!effect.canApply(ctx)) {
                if (engine.getEventPublisher() != null) {
                    engine.getEventPublisher().publish(
                            GameEventType.NOTIFICATION, matchId, playerId,
                            trainerCard.getName() + ": no se puede usar ahora");
                }
                return false;
            }
        }
        for (TrainerEffect effect : effects) {
            effect.apply(ctx);
        }
        return true;
    }

    private void handleEvolve(GameEngine engine, String matchId, MatchSnapshot snapshot,
                              TurnManager turnManager, GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String cardId = extractCardId(payload);
        String targetId = extractTargetId(payload);

        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }

        GameCard evolutionCard = findCardInHand(board, cardId);
        if (evolutionCard == null) {
            throw new InvalidActionException("Evolution card not found in hand");
        }
        if (!(evolutionCard instanceof PokemonCard evolution)) {
            throw new InvalidActionException("Card is not a Pokemon");
        }

        PokemonInPlay target = findPokemonOnBoard(board, targetId);
        if (target == null) {
            throw new InvalidActionException("Base Pokemon not found on your board");
        }

        String evolvesFrom = evolution.getEvolvesFrom();
        if (evolvesFrom != null && !evolvesFrom.equalsIgnoreCase(target.getCard().getName())) {
            throw new InvalidActionException("Cannot evolve " + target.getCard().getName()
                    + " into " + evolution.getName() + " (evolves from " + evolvesFrom + ")");
        }

        board.getHand().remove(evolutionCard);
        board.getDiscardPile().add(target.getCard());

        int oldMaxHp = target.getCard().getHp();
        int damageTaken = Math.max(0, oldMaxHp - target.getCurrentHp());
        target.setCard(evolution);
        target.setCurrentHp(Math.max(0, evolution.getHp() - damageTaken));

        target.getSpecialConditions().clear();
        turnManager.getContext().setHasEvolved(true);

        engine.getEventPublisher().publish(GameEventType.POKEMON_EVOLVED, matchId, playerId,
                "Evolved to " + evolution.getName());
    }

    private void handleRetreat(GameEngine engine, String matchId, MatchSnapshot snapshot,
                               TurnManager turnManager, GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String targetId = extractTargetId(payload);

        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }

        PokemonInPlay active = board.getActivePokemon();
        if (active == null) {
            throw new InvalidActionException("No active Pokemon to retreat");
        }

        if (active.getSpecialConditions().contains(SpecialCondition.ASLEEP)) {
            throw new InvalidActionException("Cannot retreat while asleep");
        }
        if (active.getSpecialConditions().contains(SpecialCondition.PARALYZED)) {
            throw new InvalidActionException("Cannot retreat while paralyzed");
        }

        int retreatCost = active.getCard().getRetreatCost() != null
                ? active.getCard().getRetreatCost()
                : 0;

        GameBoard gb = snapshot.getBoard();
        if (gb != null && gb.getSharedStadium() != null) {
            ar.edu.utn.frc.tup.piii.engine.effects.continuous.StadiumEffect stadium =
                    ar.edu.utn.frc.tup.piii.engine.effects.continuous.StadiumEffectResolver.resolve(
                            gb.getSharedStadium());
            retreatCost = stadium.modifyRetreatCost(active, retreatCost);
        }

        if (active.getAttachedEnergy().size() < retreatCost) {
            throw new InvalidActionException("Not enough energy to retreat (need " + retreatCost + ")");
        }

        PokemonInPlay toSwap = null;
        if (targetId != null) {
            toSwap = board.getBench().stream()
                    .filter(p -> p.getCard().getId().equals(targetId))
                    .findFirst().orElse(null);
        }
        if (toSwap == null && !board.getBench().isEmpty()) {
            toSwap = board.getBench().get(0);
        }
        if (toSwap == null) {
            throw new InvalidActionException("No Pokemon on bench to retreat to");
        }

        for (int i = 0; i < retreatCost && !active.getAttachedEnergy().isEmpty(); i++) {
            EnergyCard discarded = active.getAttachedEnergy().remove(0);
            board.getDiscardPile().add(discarded);
        }

        board.getBench().remove(toSwap);
        board.getBench().add(active);
        active.setActive(false);
        active.getSpecialConditions().clear();
        board.setActivePokemon(toSwap);
        toSwap.setActive(true);

        turnManager.getContext().setHasRetreated(true);

        engine.getEventPublisher().publish(GameEventType.POKEMON_RETREATED, matchId, playerId,
                "Retreated to " + toSwap.getCard().getName());
    }

    private void handleUseAbility(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                   GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String cardId = extractCardId(payload);

        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }

        PokemonInPlay pokemon = findPokemonOnBoard(board, cardId);
        if (pokemon == null) {
            throw new InvalidActionException("Pokemon not found on your board");
        }

        if (pokemon.getCard().getAbilities() == null || pokemon.getCard().getAbilities().isEmpty()) {
            throw new InvalidActionException("Pokemon has no abilities");
        }

        TurnManager turnManager = turnManagers.get(matchId);
        if (turnManager != null && turnManager.getContext().isHasUsedAbility()) {
            throw new InvalidActionException("Already used an ability this turn");
        }

        GameBoard gameBoard = snapshot.getBoard();
        PlayerBoard opponentBoard = getOpponentBoard(snapshot, playerId);

        int abilityIndex = 0;
        if (payload.containsKey("abilityIndex")) {
            try {
                abilityIndex = Integer.parseInt(payload.get("abilityIndex").toString());
            } catch (NumberFormatException e) {
                abilityIndex = 0;
            }
        }
        List<Ability> abilities = pokemon.getCard().getAbilities();
        if (abilityIndex < 0 || abilityIndex >= abilities.size()) {
            throw new InvalidActionException("Invalid ability index");
        }
        Ability ability = abilities.get(abilityIndex);
        List<AbilityEffect> effects = abilityEffectResolver.resolve(ability);

        if (!effects.isEmpty()) {
            AbilityEffectContext ctx = new AbilityEffectContext(
                matchId, playerId, pokemon,
                gameBoard, board, opponentBoard,
                engine.getEventPublisher(),
                turnManager != null ? turnManager.getPhase() : null
            );

            String sourceTargetId = payload.containsKey("sourceTargetId")
                    ? payload.get("sourceTargetId").toString() : null;
            String targetId = payload.containsKey("targetId")
                    ? payload.get("targetId").toString() : null;
            ctx.setSourceTargetId(sourceTargetId);
            ctx.setTargetId(targetId);

            for (AbilityEffect effect : effects) {
                if (effect.canApply(ctx)) {
                    effect.apply(ctx);
                }
            }
        }

        if (turnManager != null) {
            String effectText = ability.getEffectText() != null ? ability.getEffectText().toLowerCase() : "";
            boolean isMultiUse = effectText.contains("as often as you like")
                    || effectText.contains("veces que quieras")
                    || effectText.contains("any number of times");
            if (!isMultiUse) {
                turnManager.getContext().setHasUsedAbility(true);
            }
        }

        engine.getEventPublisher().publish(GameEventType.ABILITY_USED, matchId, playerId,
                "Ability used: " + ability.getName());
    }

    private void handlePlacePokemon(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                    GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String cardId = extractCardId(payload);

        PlayerBoard board = getPlayerBoard(snapshot, playerId);
        if (board == null) {
            throw new InvalidActionException("Player board not found");
        }

        GameCard card = findCardInHand(board, cardId);
        if (card == null) {
            throw new InvalidActionException("Pokemon card not found in hand");
        }
        if (!(card instanceof PokemonCard pokemon)) {
            throw new InvalidActionException("Card is not a Pokemon");
        }

        if (pokemon.getStage() != PokemonStage.BASIC && pokemon.getStage() != PokemonStage.EX) {
            throw new InvalidActionException("Only Basic Pokemon can be placed on the bench during active play");
        }

        if (board.getBench().size() >= 5) {
            throw new InvalidActionException("Bench is full (max 5)");
        }

        board.getHand().remove(card);
        PokemonInPlay pip = new PokemonInPlay(pokemon);
        pip.setActive(false);
        board.getBench().add(pip);

        engine.getEventPublisher().publish(GameEventType.POKEMON_PLACED, matchId, playerId,
                "Placed " + pokemon.getName() + " on bench");
    }

    private boolean handleBetweenTurnsKO(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                          GameBoard board, TurnManager turnManager) {
        PlayerBoard currentBoard = getPlayerBoard(snapshot, turnManager.getCurrentPlayerId());
        if (currentBoard == null) return false;

        PokemonInPlay active = currentBoard.getActivePokemon();
        if (active != null && active.isKnockedOut()) {
            String opponentId = snapshot.getPlayer1Id().equals(turnManager.getCurrentPlayerId())
                    ? snapshot.getPlayer2Id() : snapshot.getPlayer1Id();
            handleKnockout(engine, matchId, snapshot, active, currentBoard, opponentId);

            PlayerBoard opponentBoard = getPlayerBoard(snapshot, opponentId);
            if ((opponentBoard != null && opponentBoard.getPendingPrizeCount() > 0)
                    || currentBoard.getPendingPrizeCount() > 0) {
                // KO at start of turn (poison/burn): pause in BETWEEN_TURNS so the
                // opponent can take prizes (TAKE_PRIZE) and this player can promote (SET_ACTIVE)
                turnManager.setPhase(TurnPhase.BETWEEN_TURNS);
                engine.saveSnapshot(matchId, snapshot);
                return true;
            }
        }

        boolean needsPromotion = currentBoard.getActivePokemon() == null
                && !currentBoard.getBench().isEmpty();
        if (needsPromotion) {
            turnManager.setPhase(TurnPhase.BETWEEN_TURNS);
        }
        return needsPromotion;
    }

    private boolean isStillActive(GameEngine engine, String matchId) {
        MatchState state = engine.getCurrentState(matchId);
        return state != null && state.getPhase() == GamePhase.ACTIVE;
    }

    private String extractCardId(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("cardId")) {
            throw new InvalidActionException("cardId is required");
        }
        return (String) payload.get("cardId");
    }

    private String extractTargetId(Map<String, Object> payload) {
        if (payload != null && payload.containsKey("targetId")) {
            return (String) payload.get("targetId");
        }
        return null;
    }

    private void handleKnockout(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                PokemonInPlay koPokemon,
                                PlayerBoard koPlayerBoard, String attackerPlayerId) {
        koPlayerBoard.getDiscardPile().add(koPokemon.getCard());
        for (EnergyCard energy : koPokemon.getAttachedEnergy()) {
            koPlayerBoard.getDiscardPile().add(energy);
        }
        koPokemon.getAttachedEnergy().clear();

        if (koPokemon.getTool() != null) {
            koPlayerBoard.getDiscardPile().add(koPokemon.getTool());
            koPokemon.setTool(null);
        }

        if (koPokemon.isActive()) {
            koPlayerBoard.setActivePokemon(null);
            koPokemon.setActive(false);
        } else {
            koPlayerBoard.getBench().remove(koPokemon);
        }

        PlayerBoard attackerBoard = getPlayerBoard(snapshot, attackerPlayerId);
        if (attackerBoard != null && !attackerBoard.getPrizeCards().isEmpty()) {
            PokemonStage koStage = koPokemon.getCard().getStage();
            int prizeCount = (koStage == PokemonStage.EX || koStage == PokemonStage.MEGA) ? 2 : 1;
            prizeCount = Math.min(prizeCount, attackerBoard.getPrizeCards().size());
            attackerBoard.setPendingPrizeCount(prizeCount);
            engine.getEventPublisher().publish(GameEventType.PRIZE_NEEDED, matchId, attackerPlayerId,
                    "Select " + prizeCount + " prize card(s) to take");
        }

        engine.getEventPublisher().publish(GameEventType.POKEMON_KNOCKED_OUT, matchId,
                koPlayerBoard.getPlayerId(),
                koPokemon.getCard().getName() + " was knocked out");
    }

    private void handleSelectTarget(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                     TurnManager turnManager, GameBoard board,
                                     GameActionMessage action, String playerId) {
        PlayerBoard playerBoard = getPlayerBoard(snapshot, playerId);
        if (playerBoard == null) {
            throw new InvalidActionException("Player board not found");
        }
        if (playerBoard.getPendingMultiCoinTargets() <= 0) {
            throw new InvalidActionException("No pending target selection");
        }

        Map<String, Object> payload = action.getPayload();
        String targetId = payload != null ? (String) payload.get("targetId") : null;
        if (targetId == null || targetId.isEmpty()) {
            throw new InvalidActionException("targetId is required");
        }

        PokemonInPlay target = findBenchPokemon(playerBoard, targetId);
        if (target == null) {
            throw new InvalidActionException("Target not found in bench");
        }

        String energyType = playerBoard.getPendingMultiCoinEnergyType();
        Iterator<GameCard> discardIt = playerBoard.getDiscardPile().iterator();
        boolean attached = false;
        while (discardIt.hasNext()) {
            GameCard card = discardIt.next();
            if (card instanceof EnergyCard e
                    && (energyType == null || energyType.isEmpty()
                    || e.getType().name().equalsIgnoreCase(energyType))) {
                discardIt.remove();
                target.getAttachedEnergy().add(e);
                attached = true;
                break;
            }
        }

        if (!attached) {
            engine.getEventPublisher().publish(GameEventType.NOTIFICATION, matchId, playerId,
                    "No matching energy card found in discard");
        }

        int remaining = playerBoard.getPendingMultiCoinTargets() - 1;
        playerBoard.setPendingMultiCoinTargets(remaining);

        if (remaining > 0) {
            engine.getEventPublisher().publish(GameEventType.NOTIFICATION, matchId, playerId,
                    "Select target for energy attachment (" + remaining + " remaining)");
            engine.saveSnapshot(matchId, snapshot);
        } else {
            playerBoard.setPendingMultiCoinEnergyType(null);
            continueTurnAfter(engine, matchId, snapshot, turnManager, board);
        }
    }

    private void handleTakePrize(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                  TurnManager turnManager, GameBoard board,
                                  GameActionMessage action, String playerId) {
        Map<String, Object> payload = action.getPayload();
        String cardId = extractCardId(payload);

        PlayerBoard playerBoard = getPlayerBoard(snapshot, playerId);
        if (playerBoard == null) {
            throw new InvalidActionException("Player board not found");
        }
        if (playerBoard.getPendingPrizeCount() <= 0) {
            throw new InvalidActionException("No pending prizes to take");
        }

        GameCard selected = null;
        for (GameCard prize : playerBoard.getPrizeCards()) {
            if (prize.getId().equals(cardId)) {
                selected = prize;
                break;
            }
        }
        if (selected == null) {
            throw new InvalidActionException("Prize card not found with id: " + cardId);
        }

        playerBoard.getPrizeCards().remove(selected);
        playerBoard.getHand().add(selected);
        playerBoard.setPrizeCardsTaken(playerBoard.getPrizeCardsTaken() + 1);
        playerBoard.setPendingPrizeCount(playerBoard.getPendingPrizeCount() - 1);

        engine.getEventPublisher().publish(GameEventType.PRIZE_TAKEN, matchId, playerId,
                "Took a prize card: " + selected.getName());

        if (playerBoard.getPendingPrizeCount() == 0) {
            if (turnManager.getPhase() == TurnPhase.ATTACK
                    || turnManager.getPhase() == TurnPhase.BETWEEN_TURNS) {
                continueTurnAfter(engine, matchId, snapshot, turnManager, board);
            }
        }
    }

    private PlayerBoard getOpponentBoard(MatchSnapshot snapshot, String playerId) {
        GameBoard board = snapshot.getBoard();
        if (board == null) return null;
        if (board.getPlayer1Board() != null && !playerId.equals(board.getPlayer1Board().getPlayerId())) {
            return board.getPlayer1Board();
        }
        if (board.getPlayer2Board() != null && !playerId.equals(board.getPlayer2Board().getPlayerId())) {
            return board.getPlayer2Board();
        }
        return null;
    }

    private PlayerBoard getPlayerBoard(MatchSnapshot snapshot, String playerId) {
        GameBoard board = snapshot.getBoard();
        if (board == null) return null;
        if (board.getPlayer1Board() != null && playerId.equals(board.getPlayer1Board().getPlayerId())) {
            return board.getPlayer1Board();
        }
        if (board.getPlayer2Board() != null && playerId.equals(board.getPlayer2Board().getPlayerId())) {
            return board.getPlayer2Board();
        }
        return null;
    }

    private GameCard findCardInHand(PlayerBoard board, String cardId) {
        for (GameCard card : board.getHand()) {
            if (card.getId().equals(cardId)) {
                return card;
            }
        }
        return null;
    }

    private PokemonInPlay findPokemonOnBoard(PlayerBoard board, String cardId) {
        if (board.getActivePokemon() != null && board.getActivePokemon().getCard().getId().equals(cardId)) {
            return board.getActivePokemon();
        }
        for (PokemonInPlay bench : board.getBench()) {
            if (bench.getCard().getId().equals(cardId)) {
                return bench;
            }
        }
        return null;
    }

    private PokemonInPlay findBenchPokemon(PlayerBoard board, String cardId) {
        for (PokemonInPlay bench : board.getBench()) {
            if (bench.getCard().getId().equals(cardId)) {
                return bench;
            }
        }
        return null;
    }
}