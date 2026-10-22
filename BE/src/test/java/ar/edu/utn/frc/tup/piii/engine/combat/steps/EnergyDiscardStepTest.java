package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnergyDiscardStepTest {

    private static final String P1_ID = "p1";
    private static final String P2_ID = "p2";

    private EnergyDiscardStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        sut = new EnergyDiscardStep();
        nextExecuted = false;
        sut.setNext(new AttackStep() {
            @Override public void execute(AttackContext context) { nextExecuted = true; }
            @Override public void setNext(AttackStep next) {}
        });
    }

    @Test
    void execute_whenNoCost_delegates() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        Attack noCost = new Attack("Free", List.of(), 0, null);
        ctx.setSelectedAttack(noCost);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_withCost_discardsMatchingEnergy() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));
        Attack attack = new Attack("Fire Punch", List.of(EnergyType.FIRE, EnergyType.COLORLESS), 30, null);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(0, attacker.getAttachedEnergy().size());
        assertEquals(2, p1Board.getDiscardPile().size());
    }

    @Test
    void execute_withWildcard_discardsWildcardWhenNoSpecificMatch() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        EnergyCard rainbow = GameTestFactory.createBasicEnergyCard("rainbow1", EnergyType.COLORLESS);
        rainbow.setWildcard(true);
        rainbow.setProvidedEnergies(List.of(EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(rainbow);
        // Cost: WATER (specific) + COLORLESS — only wildcard available for WATER
        Attack attack = new Attack("Wildcard Hit", List.of(EnergyType.WATER, EnergyType.COLORLESS), 30, null);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(attacker.getAttachedEnergy().isEmpty(),
                "All energy (including wildcard) should be discarded");
        assertEquals(1, p1Board.getDiscardPile().size(),
                "Wildcard energy should be discarded to discard pile");
    }
}
