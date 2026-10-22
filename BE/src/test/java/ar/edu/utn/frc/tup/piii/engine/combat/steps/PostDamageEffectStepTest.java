package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostDamageEffectStepTest {

    private PostDamageEffectStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        sut = new PostDamageEffectStep();
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

    @Test
    void execute_appliesDamage() {
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.GRASS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setDefender(defender);
        ctx.setFinalDamage(30);

        sut.execute(ctx);

        assertEquals(70, defender.getCurrentHp());
        assertEquals(3, defender.getDamageCounters());
        assertTrue(nextExecuted);
    }

    @Test
    void execute_appliesEffects() {
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setDefender(defender);
        ctx.setFinalDamage(10);
        ctx.setEffectsToApply(List.of(SpecialCondition.BURNED));

        sut.execute(ctx);

        assertTrue(defender.getSpecialConditions().contains(SpecialCondition.BURNED));
    }

    @Test
    void execute_zeroDamage_doesNotApplyDamage() {
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.GRASS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setDefender(defender);
        ctx.setFinalDamage(0);

        sut.execute(ctx);

        assertEquals(100, defender.getCurrentHp());
        assertTrue(nextExecuted);
    }

    @Test
    void execute_whenAttackFailed_doesNothing() {
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        int originalHp = defender.getCurrentHp();
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setDefender(defender);
        ctx.setFinalDamage(50);
        ctx.setAttackSuccessful(false);

        sut.execute(ctx);

        assertEquals(originalHp, defender.getCurrentHp());
        assertFalse(nextExecuted);
    }

    @Test
    void execute_nullEffects_doesNotFail() {
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setDefender(defender);
        ctx.setFinalDamage(20);
        ctx.setEffectsToApply(null);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_pokeBodyPreventsDamageUnderThreshold() {
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Shedinja", 100, EnergyType.GRASS);
        defenderCard.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.models.Ability(
                "Wonder Guard", ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_BODY,
                "Prevents all damage done by attacks if the damage is 30 or less.")));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        GameBoard board = GameTestFactory.createGameBoard("player-1", attacker, "player-2", defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, null, board);
        ctx.setCurrentPlayerId("player-1");
        ctx.setMatchId("match-1");
        ctx.setFinalDamage(30);

        sut.execute(ctx);

        assertEquals(100, defender.getCurrentHp()); // todo el daño prevenido
        assertTrue(nextExecuted);
    }

    @Test
    void execute_pokeBodyDoesNotPreventDamageAboveThreshold() {
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Shedinja", 100, EnergyType.GRASS);
        defenderCard.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.models.Ability(
                "Wonder Guard", ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_BODY,
                "Prevents all damage done by attacks if the damage is 30 or less.")));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        GameBoard board = GameTestFactory.createGameBoard("player-2", attacker, "player-1", defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, null, board);
        ctx.setCurrentPlayerId("player-2");
        ctx.setMatchId("match-1");
        ctx.setFinalDamage(50);

        sut.execute(ctx);

        assertEquals(50, defender.getCurrentHp()); // 50 > 30, no se previene
    }

    @Test
    void execute_pokePowerAbilityIsIgnoredForDamagePrevention() {
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.GRASS);
        defenderCard.setAbilities(List.of(new ar.edu.utn.frc.tup.piii.engine.models.Ability(
                "Some Power", ar.edu.utn.frc.tup.piii.enums.AbilityType.POKE_POWER,
                "Prevents all damage if the damage is 30 or less.")));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        GameBoard board = GameTestFactory.createGameBoard("player-1", attacker, "player-2", defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, null, board);
        ctx.setCurrentPlayerId("player-1");
        ctx.setMatchId("match-1");
        ctx.setFinalDamage(20);

        sut.execute(ctx);

        assertEquals(80, defender.getCurrentHp()); // POKE_POWER no previene → daño aplicado
    }
}
