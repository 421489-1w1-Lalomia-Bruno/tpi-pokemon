package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnergyValidationStepTest {

    private EnergyValidationStep sut;
    private AttackStep nextStep;

    @BeforeEach
    void setUp() {
        sut = new EnergyValidationStep();
        nextStep = new AttackStep() {
            @Override
            public void execute(AttackContext context) {
            }
            @Override
            public void setNext(AttackStep next) {
            }
        };
        sut.setNext(nextStep);
    }

    @Test
    void execute_sufficientEnergy_continuesToNextStep() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        Attack attack = GameTestFactory.createAttack("Fire Punch", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, GameTestFactory.createPokemonInPlay(), attack, null);

        sut.execute(ctx);

        assertTrue(ctx.isAttackSuccessful());
    }

    @Test
    void execute_noCost_continuesToNextStep() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        Attack noCost = GameTestFactory.createAttack("Free Move", List.of(), 0);
        ctx.setSelectedAttack(noCost);

        sut.execute(ctx);

        assertTrue(ctx.isAttackSuccessful());
    }

    @Test
    void execute_nullAttack_continuesToNextStep() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setSelectedAttack(null);

        sut.execute(ctx);

        assertTrue(ctx.isAttackSuccessful());
    }

    @Test
    void execute_missingSpecificEnergy_fails() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        Attack attack = GameTestFactory.createAttack("Water Cannon", List.of(EnergyType.WATER, EnergyType.WATER), 60);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, GameTestFactory.createPokemonInPlay(), attack, null);

        sut.execute(ctx);

        assertFalse(ctx.isAttackSuccessful());
        assertTrue(ctx.getFailureReason().contains("Missing required energy"));
    }

    @Test
    void execute_missingColorless_fails() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        Attack attack = GameTestFactory.createAttack("Big Hit", List.of(EnergyType.FIRE, EnergyType.COLORLESS, EnergyType.COLORLESS), 70);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, GameTestFactory.createPokemonInPlay(), attack, null);

        sut.execute(ctx);

        assertFalse(ctx.isAttackSuccessful());
        assertTrue(ctx.getFailureReason().contains("Not enough energy"));
    }

    @Test
    void execute_specificAndColorless_succeeds() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.COLORLESS));
        Attack attack = GameTestFactory.createAttack("Big Hit", List.of(EnergyType.FIRE, EnergyType.COLORLESS, EnergyType.COLORLESS), 70);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, GameTestFactory.createPokemonInPlay(), attack, null);

        sut.execute(ctx);

        assertTrue(ctx.isAttackSuccessful());
    }

    @Test
    void execute_doubleColorless_forColorlessCost() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createDoubleColorlessEnergy());
        Attack attack = GameTestFactory.createAttack("Colorless Hit", List.of(EnergyType.COLORLESS, EnergyType.COLORLESS), 40);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, GameTestFactory.createPokemonInPlay(), attack, null);

        sut.execute(ctx);

        assertTrue(ctx.isAttackSuccessful());
    }

    @Test
    void execute_whenAttackAlreadyFailed_doesNotProceed() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setAttackSuccessful(false);

        sut.execute(ctx);

        assertFalse(ctx.isAttackSuccessful());
    }
}
