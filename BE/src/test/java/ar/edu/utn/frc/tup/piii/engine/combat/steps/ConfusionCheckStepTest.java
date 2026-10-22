package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfusionCheckStepTest {

    private StatusEffectManager effectManager;
    private ConfusionCheckStep sut;
    private AttackStep nextStep;
    private AttackContext context;

    @BeforeEach
    void setUp() {
        effectManager = new StatusEffectManager();
        sut = new ConfusionCheckStep(effectManager);
        nextStep = mock(AttackStep.class);

        sut.setNext(nextStep);

        context = new AttackContext();
        PokemonCard attackerCard = new PokemonCard();
        attackerCard.setHp(100);
        attackerCard.setName("Confused Pokemon");
        PokemonInPlay attacker = new PokemonInPlay(attackerCard);
        attacker.setActive(true);
        context.setAttacker(attacker);
        context.setAttackSuccessful(true);

        PokemonCard defenderCard = new PokemonCard();
        defenderCard.setHp(100);
        defenderCard.setName("Defender");
        context.setDefender(new PokemonInPlay(defenderCard));
    }

    @Test
    void execute_withoutConfused_continuesToNextStep() {
        sut.execute(context);

        assertTrue(context.isAttackSuccessful());
        assertEquals(0, context.getAttacker().getDamageCounters());
        verify(nextStep).execute(context);
    }

    @Test
    void execute_withConfusedAndHeads_continuesToNextStep() {
        effectManager.addEffect(context.getAttacker(), SpecialCondition.CONFUSED);

        for (int i = 0; i < 100; i++) {
            AttackContext ctx = createAttackContext();
            effectManager.addEffect(ctx.getAttacker(), SpecialCondition.CONFUSED);
            sut.execute(ctx);
            if (ctx.isAttackSuccessful()) {
                assertEquals(0, ctx.getAttacker().getDamageCounters());
                return;
            }
        }
    }

    @Test
    void execute_withConfusedAndTails_stopsPipeline() {
        for (int i = 0; i < 100; i++) {
            AttackContext ctx = createAttackContext();
            effectManager.addEffect(ctx.getAttacker(), SpecialCondition.CONFUSED);
            sut.execute(ctx);
            if (!ctx.isAttackSuccessful()) {
                assertEquals(3, ctx.getAttacker().getDamageCounters());
                assertNotNull(ctx.getFailureReason());
                verify(nextStep, never()).execute(ctx);
                return;
            }
        }
    }

    @Test
    void execute_whenAttackAlreadyFailed_doesNotExecuteNext() {
        context.setAttackSuccessful(false);

        sut.execute(context);

        verify(nextStep, never()).execute(context);
    }

    private AttackContext createAttackContext() {
        AttackContext ctx = new AttackContext();
        PokemonCard card = new PokemonCard();
        card.setHp(200);
        card.setName("Attacker");
        PokemonInPlay attacker = new PokemonInPlay(card);
        ctx.setAttacker(attacker);
        ctx.setAttackSuccessful(true);
        return ctx;
    }
}