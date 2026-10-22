package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.Attack;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlipEffectStepTest {

    private static final String P1 = "player-1";
    private static final String P2 = "player-2";

    private FlipEffectStep sut;
    private boolean nextExecuted;
    private GameEventPublisher publisher;

    @BeforeEach
    void setUp() {
        sut = new FlipEffectStep();
        nextExecuted = false;
        publisher = mock(GameEventPublisher.class);
        sut.setNext(new AttackStep() {
            @Override public void execute(AttackContext context) { nextExecuted = true; }
            @Override public void setNext(AttackStep next) { }
        });
    }

    private AttackContext context(String currentPlayerId, String effectText, GameBoard board) {
        PokemonInPlay attacker = board != null && board.getPlayer1Board() != null
                ? board.getPlayer1Board().getActivePokemon() : GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Coin Attack", List.of(EnergyType.COLORLESS), 0, effectText);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker,
                GameTestFactory.createPokemonInPlay(), attack, board);
        ctx.setCurrentPlayerId(currentPlayerId);
        ctx.setMatchId("match-1");
        ctx.setEventPublisher(publisher);
        return ctx;
    }

    @Test
    void execute_whenAttackFailed_doesNotProceed() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setAttackSuccessful(false);
        sut.execute(ctx);
        assertFalse(nextExecuted);
    }

    @Test
    void execute_nullEffectText_delegatesToNext() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("X", List.of(EnergyType.COLORLESS), 0, null);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker,
                GameTestFactory.createPokemonInPlay(), attack, null);
        sut.execute(ctx);
        assertTrue(nextExecuted);
    }

    @Test
    void execute_noMatchingEffect_delegatesToNext() {
        GameBoard board = GameTestFactory.createGameBoard(P1, GameTestFactory.createPokemonInPlay(),
                P2, GameTestFactory.createPokemonInPlay());
        AttackContext ctx = context(P1, "Just a plain attack with no special effect.", board);
        sut.execute(ctx);
        assertTrue(nextExecuted);
        verifyNoInteractions(publisher);
    }

    @Test
    void execute_flipCoinsEffect_appliesAndPublishesWhenNotResolvedByAttack() {
        GameBoard board = GameTestFactory.createGameBoard(P1, GameTestFactory.createPokemonInPlay(),
                P2, GameTestFactory.createPokemonInPlay());
        AttackContext ctx = context(P1, "Flip 2 coins.", board);
        ctx.setFlipResolvedByAttack(false);

        sut.execute(ctx);

        verify(publisher, atLeastOnce()).publish(any(), any(), any(), any(), any(), any(), anyInt());
        assertTrue(nextExecuted);
    }

    @Test
    void execute_flipCoinsEffect_skippedWhenResolvedByAttack() {
        GameBoard board = GameTestFactory.createGameBoard(P1, GameTestFactory.createPokemonInPlay(),
                P2, GameTestFactory.createPokemonInPlay());
        AttackContext ctx = context(P1, "Flip 2 coins.", board);
        ctx.setFlipResolvedByAttack(true);

        sut.execute(ctx);

        verifyNoInteractions(publisher);
        assertTrue(nextExecuted);
    }

    @Test
    void execute_resolvesSourceBoardWhenAttackerIsPlayer2() {
        GameBoard board = GameTestFactory.createGameBoard(P1, GameTestFactory.createPokemonInPlay(),
                P2, GameTestFactory.createPokemonInPlay());
        // attacker = player-2 active → rama else (player2Board)
        AttackContext ctx = context(P2, "Flip 1 coin.", board);
        ctx.setAttacker(board.getPlayer2Board().getActivePokemon());

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_nullBoard_stillAppliesEffect() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("X", List.of(EnergyType.COLORLESS), 0, "Flip 1 coin.");
        AttackContext ctx = GameTestFactory.createAttackContext(attacker,
                GameTestFactory.createPokemonInPlay(), attack, null);
        ctx.setCurrentPlayerId(P1);
        ctx.setMatchId("match-1");
        ctx.setEventPublisher(publisher);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }
}
