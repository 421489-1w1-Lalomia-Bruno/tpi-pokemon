package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.combat.AttackFlipResolver;
import ar.edu.utn.frc.tup.piii.engine.models.Attack;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalFlipStepTest {

    private ConditionalFlipStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        sut = new ConditionalFlipStep();
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

    @AfterEach
    void tearDown() {
        CoinFlipManager.setRng(null);
    }

    @Test
    void execute_whenNotConditionalFlip_delegatesToNext() {
        AttackContext ctx = new AttackContext();
        ctx.setAttackSuccessful(true);
        ctx.setNeedsConditionalFlip(false);
        ctx.setSelectedAttack(new Attack("Tackle", List.of(EnergyType.COLORLESS), 30, ""));

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertNull(ctx.isConditionalFlipHeads());
    }

    @Test
    void execute_whenAttackFailed_doesNotProceed() {
        AttackContext ctx = new AttackContext();
        ctx.setAttackSuccessful(false);
        ctx.setNeedsConditionalFlip(true);

        sut.execute(ctx);

        assertFalse(nextExecuted);
    }

    @Test
    void execute_whenHeads_setsBranchTextAndConditions() {
        CoinFlipManager.setRng(new Random() {
            @Override public boolean nextBoolean() { return true; }
        });

        Attack attack = new Attack("Coin Flip", List.of(EnergyType.COLORLESS), 0,
                "Flip a coin. If heads, the Defending Pokemon is now Paralyzed. If tails, nothing.");
        AttackContext ctx = new AttackContext();
        ctx.setAttackSuccessful(true);
        ctx.setNeedsConditionalFlip(true);
        ctx.setSelectedAttack(attack);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(ctx.isConditionalFlipHeads());
        assertNotNull(ctx.getBranchEffectText());
        assertFalse(ctx.getEffectsToApply().isEmpty());
        assertTrue(ctx.getEffectsToApply().stream()
                .anyMatch(sc -> sc.name().equals("PARALYZED")));
    }

    @Test
    void execute_whenHeads_withPublisher_publishesEvent() {
        CoinFlipManager.setRng(new Random() {
            @Override public boolean nextBoolean() { return true; }
        });

        Attack attack = new Attack("Coin Flip", List.of(EnergyType.COLORLESS), 0,
                "Flip a coin. If heads, Paraliza. If tails, nothing.");
        AttackContext ctx = new AttackContext();
        ctx.setAttackSuccessful(true);
        ctx.setNeedsConditionalFlip(true);
        ctx.setSelectedAttack(attack);
        ctx.setCurrentPlayerId("p1");
        ctx.setMatchId("match-1");
        ctx.setAttacker(GameTestFactory.createPokemonInPlay());

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(ctx.isConditionalFlipHeads());
    }

    @Test
    void execute_whenTails_setsBranchTextAndNoConditions() {
        CoinFlipManager.setRng(new Random() {
            @Override public boolean nextBoolean() { return false; }
        });

        Attack attack = new Attack("Coin Flip", List.of(EnergyType.COLORLESS), 0,
                "Flip a coin. If heads, Paraliza. If tails, nothing.");
        AttackContext ctx = new AttackContext();
        ctx.setAttackSuccessful(true);
        ctx.setNeedsConditionalFlip(true);
        ctx.setSelectedAttack(attack);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertFalse(ctx.isConditionalFlipHeads());
        assertNotNull(ctx.getBranchEffectText());
        assertTrue(ctx.getBranchEffectText().toLowerCase().contains("nothing"));
    }
}
