package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cobertura de ramas faltantes de VariableDamageStep (type-match, has-energy, branchText). */
class VariableDamageStepBranchTest {

    private static final String P1 = "player-1";
    private static final String P2 = "player-2";

    private VariableDamageStep sut;

    @BeforeEach
    void setUp() {
        sut = new VariableDamageStep();
        sut.setNext(new AttackStep() {
            @Override public void execute(AttackContext context) { }
            @Override public void setNext(AttackStep next) { }
        });
    }

    private AttackContext ctx(PokemonInPlay attacker, PokemonInPlay defender, Attack attack) {
        GameBoard board = GameTestFactory.createGameBoard(P1, attacker, P2, defender);
        AttackContext c = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        c.setCurrentPlayerId(P1);
        return c;
    }

    @Test
    void ifTypeMatch_bonusWhenDefenderMatchesType() {
        String txt = "20+ daño. +30 si el Activo rival es tipo Water.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Wartortle", 80, EnergyType.WATER));
        AttackContext c = ctx(attacker, defender, new Attack("X", List.of(EnergyType.COLORLESS), 0, txt));

        sut.execute(c);
        assertEquals(50, c.getBaseDamage());
    }

    @Test
    void ifTypeMatch_noBonusWhenDefenderTypeDiffers() {
        String txt = "20+ daño. +30 si el Activo rival es tipo Water.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Charmander", 60, EnergyType.FIRE));
        AttackContext c = ctx(attacker, defender, new Attack("X", List.of(EnergyType.COLORLESS), 0, txt));

        sut.execute(c);
        assertEquals(20, c.getBaseDamage());
    }

    @Test
    void ifHasEnergy_bonusWhenAttackerHasType() {
        String txt = "30+ daño. Si tiene Energía Fire, +40";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        AttackContext c = ctx(attacker, defender, new Attack("X", List.of(EnergyType.COLORLESS), 0, txt));

        sut.execute(c);
        assertEquals(70, c.getBaseDamage());
    }

    @Test
    void ifHasEnergy_noBonusWhenAttackerLacksType() {
        String txt = "30+ daño. Si tiene Energía Fire, +40";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        AttackContext c = ctx(attacker, defender, new Attack("X", List.of(EnergyType.COLORLESS), 0, txt));

        sut.execute(c);
        assertEquals(30, c.getBaseDamage());
    }

    @Test
    void branchEffectText_usedWhenPrimaryHasNoVariablePattern() {
        // Primary sin patrón; branchText con patrón N× → debe usar branchText.
        Attack attack = new Attack("X", List.of(EnergyType.COLORLESS), 0, "Sin patron variable.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive(P1, attacker);
        p1.getBench().add(GameTestFactory.createPokemonInPlay());
        p1.getBench().add(GameTestFactory.createPokemonInPlay());
        GameBoard board = GameTestFactory.createGameBoard(P1, attacker, P2, defender);
        board.setPlayer1Board(p1);
        AttackContext c = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        c.setCurrentPlayerId(P1);
        c.setBranchEffectText("20× This attack does 20 damage for each of your Benched Pokémon.");

        sut.execute(c);
        assertEquals(40, c.getBaseDamage());
    }
}
