package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.combat.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DamageCalculationStepTest {

    private DamageCalculationStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        DamageCalculator mockCalc = new DamageCalculator() {
            @Override
            public int calculateDamage(AttackContext context) {
                return 42;
            }
        };
        sut = new DamageCalculationStep(mockCalc);
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
    void execute_setsFinalDamageAndProceeds() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Pikachu", 100, EnergyType.LIGHTNING);
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Charmander", 80, EnergyType.FIRE);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Thunderbolt", List.of(EnergyType.LIGHTNING), 50);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        sut.execute(ctx);

        assertEquals(42, ctx.getFinalDamage());
        assertTrue(nextExecuted);
    }

    @Test
    void execute_whenAttackFailed_doesNotProceed() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setAttackSuccessful(false);

        sut.execute(ctx);

        assertNotNull(ctx.getFinalDamage());
        assertFalse(nextExecuted);
    }
}
