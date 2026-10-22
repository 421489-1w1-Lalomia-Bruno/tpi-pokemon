package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VariableDamageStepTest {

    private static final String PLAYER_1 = "player-1";
    private static final String PLAYER_2 = "player-2";

    private VariableDamageStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        sut = new VariableDamageStep();
        nextExecuted = false;
        sut.setNext(new AttackStep() {
            @Override
            public void execute(AttackContext context) {
                nextExecuted = true;
            }

            @Override
            public void setNext(AttackStep next) {
            }
        });
    }

    // ===== Existing tests (bench-based N×) =====

    @Test
    void delegatesToNext() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        sut.execute(ctx);
        assertTrue(nextExecuted);
    }

    @Test
    void whenAttackFailed_doesNotProceed() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setAttackSuccessful(false);
        sut.execute(ctx);
        assertFalse(nextExecuted);
    }

    @Test
    void circleCircuit_withNoBench_dealsZeroDamage() {
        String effectText = "20\u00d7 This attack does 20 damage for each of your Benched Pok\u00e9mon.";

        AttackContext ctx = createContextWithBench(effectText, 0);

        sut.execute(ctx);
        assertEquals(0, ctx.getBaseDamage());
        assertTrue(nextExecuted);
    }

    @Test
    void circleCircuit_withOneBench_deals20Damage() {
        AttackContext ctx = createContextWithBench(
            "20\u00d7 This attack does 20 damage for each of your Benched Pok\u00e9mon.", 1);

        sut.execute(ctx);
        assertEquals(20, ctx.getBaseDamage());
    }

    @Test
    void circleCircuit_withThreeBench_deals60Damage() {
        AttackContext ctx = createContextWithBench(
            "20\u00d7 This attack does 20 damage for each of your Benched Pok\u00e9mon.", 3);

        sut.execute(ctx);
        assertEquals(60, ctx.getBaseDamage());
    }

    @Test
    void circleCircuit_withFiveBench_deals100Damage() {
        AttackContext ctx = createContextWithBench(
            "20\u00d7 This attack does 20 damage for each of your Benched Pok\u00e9mon.", 5);

        sut.execute(ctx);
        assertEquals(100, ctx.getBaseDamage());
    }

    @Test
    void normalAttack_withoutVariablePattern_isUnchanged() {
        AttackContext ctx = GameTestFactory.createAttackContext(
            GameTestFactory.createPokemonInPlay(),
            GameTestFactory.createPokemonInPlay(),
            GameTestFactory.createAttack("Tackle", List.of(EnergyType.COLORLESS), 30),
            null
        );

        sut.execute(ctx);
        assertEquals(30, ctx.getBaseDamage());
    }

    @Test
    void nullEffectText_isUnchanged() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 50, null);
        AttackContext ctx = GameTestFactory.createAttackContext(
            GameTestFactory.createPokemonInPlay(),
            GameTestFactory.createPokemonInPlay(),
            attack,
            null
        );

        sut.execute(ctx);
        assertEquals(50, ctx.getBaseDamage());
    }

    @Test
    void attackWithColonPattern_onlyPrefix_isUnchanged() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
            "10\u00d7");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(PLAYER_1, attacker);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(10, ctx.getBaseDamage());
    }

    // ===== New Phase 4.4 tests =====

    @Test
    void nPlus_withoutCondition_keepsBase() {
        String effectText = "50+ da\u00f1o.";
        Attack attack = new Attack("Test", List.of(EnergyType.FIRE), 0, effectText);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(50, ctx.getBaseDamage());
        assertTrue(nextExecuted);
    }

    @Test
    void perEnergyNx_damageEqualsMultiplierTimesCount() {
        String effectText = "20\u00d7 da\u00f1o. 20 por cada Energ\u00eda unida a este Pok\u00e9mon.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.WATER));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.GRASS));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Balloon Barrage", List.of(EnergyType.COLORLESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(60, ctx.getBaseDamage());
    }

    @Test
    void perEnergyTypePlus_addsBonus() {
        String effectText = "50+ da\u00f1o. +20 por cada Energ\u00eda Fire unida a este Pok\u00e9mon.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.FIRE));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Blaze Ball", List.of(EnergyType.COLORLESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(90, ctx.getBaseDamage());
    }

    @Test
    void perEnergyBoth_addsBonus() {
        String effectText = "20+ da\u00f1o. +20 por cada Energ\u00eda unida a ambos Activos.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.DARKNESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.DARKNESS));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        defender.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.PSYCHIC));
        Attack attack = new Attack("Evil Ball", List.of(EnergyType.DARKNESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(80, ctx.getBaseDamage());
    }

    @Test
    void perCounterSelf_addsBonus() {
        String effectText = "20+ da\u00f1o. +10 por cada contador en este Pok\u00e9mon.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.setDamageCounters(3);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Rage", List.of(EnergyType.COLORLESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(50, ctx.getBaseDamage());
    }

    @Test
    void perCounterDef_addsBonus() {
        String effectText = "10+ da\u00f1o. +10 por cada contador en el Activo rival.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        defender.setDamageCounters(2);
        Attack attack = new Attack("Second Bite", List.of(EnergyType.COLORLESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(30, ctx.getBaseDamage());
    }

    @Test
    void perDistinctEnergy_addsBonus() {
        String effectText = "30+ da\u00f1o. +30 por cada tipo de Energ\u00eda B\u00e1sica diferente unida.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.WATER));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Colorful Wind", List.of(EnergyType.COLORLESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(90, ctx.getBaseDamage());
    }

    @Test
    void ifDefenderEx_bonus() {
        String effectText = "60+ da\u00f1o. +60 si el Activo rival es Pok\u00e9mon-EX.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonCard exCard = GameTestFactory.createBasicPokemonCard("Mewtwo-EX", 170, EnergyType.PSYCHIC);
        exCard.setStage(PokemonStage.EX);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(exCard);
        Attack attack = new Attack("Bite Off", List.of(EnergyType.COLORLESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(120, ctx.getBaseDamage());
    }

    @Test
    void ifDefenderEx_noBonus() {
        String effectText = "60+ da\u00f1o. +60 si el Activo rival es Pok\u00e9mon-EX.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonCard basicCard = GameTestFactory.createBasicPokemonCard("Mewtwo", 100, EnergyType.PSYCHIC);
        basicCard.setStage(PokemonStage.BASIC);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(basicCard);
        Attack attack = new Attack("Bite Off", List.of(EnergyType.COLORLESS), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(60, ctx.getBaseDamage());
    }

    @Test
    void ifLunatoneOnBench_bonus() {
        String effectText = "10+ da\u00f1o. +30 si Lunatone est\u00e1 en tu Banca.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonCard lunatoneCard = GameTestFactory.createBasicPokemonCard("Lunatone", 90, EnergyType.FIGHTING);
        PokemonInPlay lunatone = GameTestFactory.createPokemonInPlay(lunatoneCard);
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActiveAndBench(PLAYER_1, attacker, List.of(lunatone));
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        board.setPlayer1Board(p1Board);
        Attack attack = new Attack("Cosmic Spin", List.of(EnergyType.FIGHTING), 0, effectText);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(40, ctx.getBaseDamage());
    }

    @Test
    void ifSpecialCondition_bonus() {
        String effectText = "30+ da\u00f1o. +60 si el Activo rival tiene Condici\u00f3n Especial.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        defender.getSpecialConditions().add(SpecialCondition.PARALYZED);
        Attack attack = new Attack("Wake-Up Slap", List.of(EnergyType.FIGHTING), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(90, ctx.getBaseDamage());
    }

    @Test
    void ifDefenderDamaged_bonus() {
        String effectText = "80+ da\u00f1o. +40 si el Activo rival ya tiene contadores de da\u00f1o.";
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        defender.setDamageCounters(2);
        Attack attack = new Attack("Tailspin Piledriver", List.of(EnergyType.METAL), 0, effectText);
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);

        sut.execute(ctx);
        assertEquals(120, ctx.getBaseDamage());
    }

    // ===== Helpers =====

    private AttackContext createContextWithBench(String effectText, int benchSize) {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();

        Attack attack = new Attack("Circle Circuit", List.of(EnergyType.LIGHTNING), 0, effectText);

        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(PLAYER_1, attacker);
        for (int i = 0; i < benchSize; i++) {
            p1Board.getBench().add(GameTestFactory.createPokemonInPlay());
        }
        GameBoard board = GameTestFactory.createGameBoard(PLAYER_1, attacker, PLAYER_2, defender);
        board.setPlayer1Board(p1Board);

        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(PLAYER_1);
        return ctx;
    }
}
