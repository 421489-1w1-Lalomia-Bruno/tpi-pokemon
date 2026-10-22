package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.TurnContext;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleValidatorTest {

    private RuleValidator validator;
    private MatchSnapshot snapshot;
    private TurnContext turnCtx;

    @BeforeEach
    void setUp() {
        validator = new RuleValidator();
        snapshot = new MatchSnapshot();
        turnCtx = new TurnContext();
    }

    private GameActionMessage action(GameActionType type) {
        return GameActionMessage.builder()
                .matchId("match-1")
                .type(type)
                .payload(Map.of())
                .build();
    }

    // ---- null snapshot ----

    @Test
    void validateAction_nullSnapshot_throws() {
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.READY), "p1", null));
    }

    // ---- WAITING ----

    @Test
    void validateAction_waiting_readyAllowed() {
        snapshot.setStatus(MatchStatus.WAITING);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.READY), "p1", snapshot));
    }

    @Test
    void validateAction_waiting_attackRejected() {
        snapshot.setStatus(MatchStatus.WAITING);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACK), "p1", snapshot));
    }

    @Test
    void validateAction_waiting_placePokemonRejected() {
        snapshot.setStatus(MatchStatus.WAITING);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.PLACE_POKEMON), "p1", snapshot));
    }

    // ---- SETUP ----

    @Test
    void validateAction_setup_placePokemonAllowed() {
        snapshot.setStatus(MatchStatus.SETUP);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.PLACE_POKEMON), "p1", snapshot));
    }

    @Test
    void validateAction_setup_attackRejected() {
        snapshot.setStatus(MatchStatus.SETUP);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACK), "p1", snapshot));
    }

    @Test
    void validateAction_setup_readyRejected() {
        snapshot.setStatus(MatchStatus.SETUP);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.READY), "p1", snapshot));
    }

    // ---- FINISHED ----

    @Test
    void validateAction_finished_throws() {
        snapshot.setStatus(MatchStatus.FINISHED);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACK), "p1", snapshot));
    }

    @Test
    void validateAction_abandoned_throws() {
        snapshot.setStatus(MatchStatus.ABANDONED);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.READY), "p1", snapshot));
    }

    // ---- ACTIVE - null TurnContext ----

    @Test
    void validateAction_active_nullTurnContext_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        snapshot.setTurnContext(null);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
    }

    // ---- ACTIVE - player identity ----

    @Test
    void validateAction_active_wrongPlayer_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p2", snapshot));
    }

    @Test
    void validateAction_active_surrenderAlwaysAllowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.SURRENDER), "p2", snapshot));
    }

    // ---- ATTACH_ENERGY ----

    @Test
    void validateAction_attachEnergy_wrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ATTACK);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
    }

    @Test
    void validateAction_attachEnergy_alreadyAttached_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        turnCtx.setHasAttachedEnergy(true);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
    }

    @Test
    void validateAction_attachEnergy_allowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        turnCtx.setHasAttachedEnergy(false);
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
    }

    // ---- EVOLVE ----

    @Test
    void validateAction_evolve_firstTurn_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.EVOLVE), "p1", snapshot));
    }

    @Test
    void validateAction_evolve_wrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.DRAW);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.EVOLVE), "p1", snapshot));
    }

    @Test
    void validateAction_evolve_allowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        turnCtx.getCompletedFirstTurns().add("p1");
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.EVOLVE), "p1", snapshot));
    }

    // ---- ATTACK ----

    @Test
    void validateAction_attack_firstTurn_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ATTACK);
        turnCtx.setFirstTurn(true);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACK), "p1", snapshot));
    }

    @Test
    void validateAction_attack_wrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ATTACK);
        turnCtx.setFirstTurn(false);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACK), "p1", snapshot));
    }

    @Test
    void validateAction_attack_allowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        turnCtx.setFirstTurn(false);
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.ATTACK), "p1", snapshot));
    }

    // ---- RETREAT ----

    @Test
    void validateAction_retreat_alreadyRetreated_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        turnCtx.setHasRetreated(true);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.RETREAT), "p1", snapshot));
    }

    @Test
    void validateAction_retreat_allowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        turnCtx.setHasRetreated(false);
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.RETREAT), "p1", snapshot));
    }

    // ---- PLAY_TRAINER ----

    @Test
    void validateAction_playTrainer_wrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ATTACK);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.PLAY_TRAINER), "p1", snapshot));
    }

    @Test
    void validateAction_playTrainer_allowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.PLAY_TRAINER), "p1", snapshot));
    }

    // ---- PASS_TURN ----

    @Test
    void validateAction_passTurn_wrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.DRAW);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.PASS_TURN), "p1", snapshot));
    }

    // ---- SET_ACTIVE ----

    @Test
    void validateAction_setActive_wrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.SET_ACTIVE), "p1", snapshot));
    }

    // ---- USE_ABILITY ----

    @Test
    void validateAction_useAbility_drawPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.DRAW);
        snapshot.setTurnContext(turnCtx);

        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.USE_ABILITY), "p1", snapshot));
    }

    @Test
    void validateAction_useAbility_allowedInActions() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.USE_ABILITY), "p1", snapshot));
    }

    // ---- SET_ACTIVE ----

    @Test
    void validateAction_setActive_allowedInBetweenTurns() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.BETWEEN_TURNS);
        snapshot.setTurnContext(turnCtx);

        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.SET_ACTIVE), "p1", snapshot));
    }

    // ---- validatePlayTrainer ----

    @Test
    void validatePlayTrainer_supporterAllowed() {
        turnCtx.setHasPlayedSupporter(false);
        assertDoesNotThrow(() -> validator.validatePlayTrainer(TrainerSubtype.SUPPORTER, turnCtx));
    }

    @Test
    void validatePlayTrainer_supporterRepeated_throws() {
        turnCtx.setHasPlayedSupporter(true);
        assertThrows(InvalidActionException.class,
                () -> validator.validatePlayTrainer(TrainerSubtype.SUPPORTER, turnCtx));
    }

    @Test
    void validatePlayTrainer_stadiumAllowed() {
        turnCtx.setHasPlayedStadium(false);
        assertDoesNotThrow(() -> validator.validatePlayTrainer(TrainerSubtype.STADIUM, turnCtx));
    }

    @Test
    void validatePlayTrainer_stadiumRepeated_throws() {
        turnCtx.setHasPlayedStadium(true);
        assertThrows(InvalidActionException.class,
                () -> validator.validatePlayTrainer(TrainerSubtype.STADIUM, turnCtx));
    }

    @Test
    void validatePlayTrainer_itemAlwaysAllowed() {
        assertDoesNotThrow(() -> validator.validatePlayTrainer(TrainerSubtype.ITEM, turnCtx));
        assertDoesNotThrow(() -> validator.validatePlayTrainer(TrainerSubtype.TOOL, turnCtx));
    }

    // ---- status mapping ----

    @Test
    void validateAction_nullStatus_mapsToWaiting() {
        snapshot.setStatus(null);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.READY), "p1", snapshot));
    }

    // ---- CONFIRM_SETUP ----

    @Test
    void validateAction_setup_confirmSetupAllowed() {
        snapshot.setStatus(MatchStatus.SETUP);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.CONFIRM_SETUP), "p1", snapshot));
    }

    // ---- hasPendingReveal ----

    private void setupPendingReveal(boolean pending, String playerId) {
        PlayerBoard pb = new PlayerBoard();
        pb.setPlayerId(playerId);
        pb.setHasPendingReveal(pending);

        GameBoard board = new GameBoard();
        board.setPlayer1Board(pb);
        snapshot.setBoard(board);

        turnCtx.setCurrentPlayerId(playerId);
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);
        snapshot.setStatus(MatchStatus.ACTIVE);
    }

    @Test
    void validateAction_pendingReveal_blocksOtherActions() {
        setupPendingReveal(true, "p1");
        InvalidActionException ex = assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
        assertTrue(ex.getMessage().contains("pending reveal"));
    }

    @Test
    void validateAction_pendingReveal_allowsResolveReveal() {
        setupPendingReveal(true, "p1");
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.RESOLVE_REVEAL), "p1", snapshot));
    }

    @Test
    void validateAction_pendingReveal_allowsSurrender() {
        setupPendingReveal(true, "p2");
        turnCtx.setCurrentPlayerId("p1");
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.SURRENDER), "p2", snapshot));
    }

    @Test
    void validateAction_noPendingReveal_proceeds() {
        setupPendingReveal(false, "p1");
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
    }

    @Test
    void validateAction_nullBoard_noPendingReveal() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        snapshot.setBoard(null);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
    }

    @Test
    void validateAction_playerNotOnBoard_noPendingReveal() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        GameBoard board = new GameBoard();
        PlayerBoard pb = new PlayerBoard();
        pb.setPlayerId("other");
        board.setPlayer1Board(pb);
        snapshot.setBoard(board);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p1", snapshot));
    }

    // ---- PLACE_POKEMON in ACTIVE ----

    @Test
    void validateAction_placePokemonInActions_allowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.PLACE_POKEMON), "p1", snapshot));
    }

    @Test
    void validateAction_placePokemonWrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.DRAW);
        snapshot.setTurnContext(turnCtx);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.PLACE_POKEMON), "p1", snapshot));
    }

    // ---- TAKE_PRIZE ----

    @Test
    void validateAction_takePrize_alwaysAllowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.BETWEEN_TURNS);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.TAKE_PRIZE), "p1", snapshot));
    }

    // ---- RESOLVE_REVEAL ----

    @Test
    void validateAction_resolveReveal_alwaysAllowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.DRAW);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.RESOLVE_REVEAL), "p1", snapshot));
    }

    // ---- RETREAT wrong phase ----

    @Test
    void validateAction_retreatWrongPhase_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ATTACK);
        snapshot.setTurnContext(turnCtx);
        assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.RETREAT), "p1", snapshot));
    }

    // ---- PASS_TURN allowed ----

    @Test
    void validateAction_passTurn_allowed() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.PASS_TURN), "p1", snapshot));
    }

    // ---- USE_ABILITY in BETWEEN_TURNS ----

    @Test
    void validateAction_useAbility_allowedInBetweenTurns() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.BETWEEN_TURNS);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.USE_ABILITY), "p1", snapshot));
    }

    // ---- unknown action type ----

    @Test
    void validateAction_unknownType_throws() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);
        GameActionMessage unknown = GameActionMessage.builder()
                .matchId("match-1")
                .type(null)
                .payload(Map.of())
                .build();
        assertThrows(NullPointerException.class,
                () -> validator.validateAction(unknown, "p1", snapshot));
    }

    // ---- pendingReveal p2 branch ----

    @Test
    void validateAction_pendingReveal_player2_blocksOtherActions() {
        snapshot.setStatus(MatchStatus.ACTIVE);

        PlayerBoard pb1 = new PlayerBoard();
        pb1.setPlayerId("p1");
        pb1.setHasPendingReveal(false);
        PlayerBoard pb2 = new PlayerBoard();
        pb2.setPlayerId("p2");
        pb2.setHasPendingReveal(true);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(pb1);
        board.setPlayer2Board(pb2);
        snapshot.setBoard(board);
        turnCtx.setCurrentPlayerId("p2");
        turnCtx.setPhase(TurnPhase.ACTIONS);
        snapshot.setTurnContext(turnCtx);

        InvalidActionException ex = assertThrows(InvalidActionException.class,
                () -> validator.validateAction(action(GameActionType.ATTACH_ENERGY), "p2", snapshot));
        assertTrue(ex.getMessage().contains("pending reveal"));
    }

    // ---- USE_ABILITY in ATTACK phase ----

    @Test
    void validateAction_useAbility_allowedInAttackPhase() {
        snapshot.setStatus(MatchStatus.ACTIVE);
        turnCtx.setCurrentPlayerId("p1");
        turnCtx.setPhase(TurnPhase.ATTACK);
        snapshot.setTurnContext(turnCtx);
        assertDoesNotThrow(() -> validator.validateAction(action(GameActionType.USE_ABILITY), "p1", snapshot));
    }
}
