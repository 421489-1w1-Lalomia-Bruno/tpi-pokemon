package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.TurnContext;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.springframework.stereotype.Component;

@Component
public class RuleValidator {

    public void validateAction(GameActionMessage action, String playerId, MatchSnapshot snapshot) {
        if (snapshot == null) {
            throw new InvalidActionException("Match not found");
        }

        GamePhase gamePhase = mapStatusToPhase(snapshot.getStatus());
        GameActionType actionType = action.getType();

        switch (gamePhase) {
            case WAITING:
                if (actionType != GameActionType.READY) {
                    throw new InvalidActionException("Only READY action is allowed in WAITING phase");
                }
                return;

            case SETUP:
                if (actionType != GameActionType.PLACE_POKEMON && actionType != GameActionType.CONFIRM_SETUP) {
                    throw new InvalidActionException("Only PLACE_POKEMON or CONFIRM_SETUP actions are allowed in SETUP phase");
                }
                return;

            case FINISHED:
                throw new InvalidActionException("Game is already finished");

            case ACTIVE:
                validateActiveAction(actionType, playerId, snapshot);
                return;
            default:
                throw new InvalidActionException("Unknown game phase: " + gamePhase);
        }
    }

    public void validatePlayTrainer(TrainerSubtype subtype, TurnContext turnCtx) {
        switch (subtype) {
            case SUPPORTER:
                if (turnCtx.isHasPlayedSupporter()) {
                    throw new InvalidActionException("Already played a Supporter this turn (max 1 per turn)");
                }
                break;
            case STADIUM:
                if (turnCtx.isHasPlayedStadium()) {
                    throw new InvalidActionException("Already played a Stadium this turn (max 1 per turn)");
                }
                break;
            case ITEM:
            case TOOL:
                break;
            default:
                break;
        }
    }

    private void validateActiveAction(GameActionType actionType, String playerId, MatchSnapshot snapshot) {
        TurnContext turnCtx = snapshot.getTurnContext();
        if (turnCtx == null) {
            throw new InvalidActionException("Game has no active turn context");
        }

        if (!playerId.equals(turnCtx.getCurrentPlayerId()) && actionType != GameActionType.SURRENDER && actionType != GameActionType.SET_ACTIVE && actionType != GameActionType.TAKE_PRIZE) {
            throw new InvalidActionException("It is not your turn");
        }

        boolean hasPendingReveal = hasPendingReveal(snapshot, playerId);
        if (hasPendingReveal && actionType != GameActionType.RESOLVE_REVEAL && actionType != GameActionType.SURRENDER) {
            throw new InvalidActionException("You must resolve the pending reveal first");
        }

        TurnPhase phase = turnCtx.getPhase();

        switch (actionType) {
            case SURRENDER:
                return;

            case ATTACH_ENERGY:
                if (phase != TurnPhase.ACTIONS) {
                    throw new InvalidActionException("Can only attach energy during the ACTIONS phase");
                }
                if (turnCtx.isHasAttachedEnergy()) {
                    throw new InvalidActionException("Already attached energy this turn (max 1 per turn)");
                }
                break;

            case PLAY_TRAINER:
                if (phase != TurnPhase.ACTIONS) {
                    throw new InvalidActionException("Can only play Trainer cards during the ACTIONS phase");
                }
                break;

            case EVOLVE:
                if (phase != TurnPhase.ACTIONS) {
                    throw new InvalidActionException("Can only evolve during the ACTIONS phase");
                }
                if (turnCtx.getCompletedFirstTurns() == null || !turnCtx.getCompletedFirstTurns().contains(playerId)) {
                    throw new InvalidActionException("Cannot evolve on your first turn of the game");
                }
                break;

            case RETREAT:
                if (phase != TurnPhase.ACTIONS) {
                    throw new InvalidActionException("Can only retreat during the ACTIONS phase");
                }
                if (turnCtx.isHasRetreated()) {
                    throw new InvalidActionException("Already retreated this turn (max 1 per turn)");
                }
                break;

            case ATTACK:
                if (phase != TurnPhase.ACTIONS) {
                    throw new InvalidActionException("Can only attack during the ACTIONS phase");
                }
                if (turnCtx.isFirstTurn()) {
                    throw new InvalidActionException("Cannot attack on the first turn of the game");
                }
                break;

            case USE_ABILITY:
                if (phase == TurnPhase.DRAW) {
                    throw new InvalidActionException("Cannot use abilities during the DRAW phase");
                }
                break;

            case PASS_TURN:
                if (phase != TurnPhase.ACTIONS) {
                    throw new InvalidActionException("Can only pass the turn during the ACTIONS phase");
                }
                break;

            case PLACE_POKEMON:
                if (phase != TurnPhase.ACTIONS) {
                    throw new InvalidActionException("Can only place Pokemon during the ACTIONS phase");
                }
                break;

            case SET_ACTIVE:
                if (phase != TurnPhase.BETWEEN_TURNS) {
                    throw new InvalidActionException("Can only set active Pokemon during BETWEEN_TURNS phase");
                }
                break;

            case TAKE_PRIZE:
                break;

            case RESOLVE_REVEAL:
                break;

            default:
                throw new InvalidActionException("Unknown action type: " + actionType);
        }
    }

    private boolean hasPendingReveal(MatchSnapshot snapshot, String playerId) {
        if (snapshot.getBoard() == null) return false;
        if (snapshot.getBoard().getPlayer1Board() != null
                && playerId.equals(snapshot.getBoard().getPlayer1Board().getPlayerId())) {
            return snapshot.getBoard().getPlayer1Board().isHasPendingReveal();
        }
        if (snapshot.getBoard().getPlayer2Board() != null
                && playerId.equals(snapshot.getBoard().getPlayer2Board().getPlayerId())) {
            return snapshot.getBoard().getPlayer2Board().isHasPendingReveal();
        }
        return false;
    }

    private GamePhase mapStatusToPhase(ar.edu.utn.frc.tup.piii.enums.MatchStatus status) {
        if (status == null) return GamePhase.WAITING;
        return switch (status) {
            case WAITING -> GamePhase.WAITING;
            case SETUP -> GamePhase.SETUP;
            case ACTIVE -> GamePhase.ACTIVE;
            case FINISHED, ABANDONED -> GamePhase.FINISHED;
        };
    }
}
