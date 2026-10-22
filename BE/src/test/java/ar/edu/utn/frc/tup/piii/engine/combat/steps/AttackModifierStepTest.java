package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackModifierStepTest {

    private AttackModifierStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        sut = new AttackModifierStep();
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
    void execute_delegatesToNext() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        sut.execute(ctx);
        assertTrue(nextExecuted);
    }

    @Test
    void execute_whenAttackFailed_doesNotProceed() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setAttackSuccessful(false);
        sut.execute(ctx);
        assertFalse(nextExecuted);
    }

    @Test
    void execute_muscleBandAddsDamage() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        TrainerCard muscleBand = new TrainerCard();
        muscleBand.setId("mb-1");
        muscleBand.setName("Muscle Band");
        muscleBand.setSubtype(TrainerSubtype.TOOL);
        muscleBand.setEffect("[\"The attacks of the Pokémon this card is attached to do 20 " +
                "more damage to your opponent's Active Pokémon (before applying Weakness and Resistance).\"]");
        attacker.setTool(muscleBand);

        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = GameTestFactory.createAttack("Tackle", List.of(EnergyType.COLORLESS), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        sut.execute(ctx);

        assertEquals(50, ctx.getBaseDamage());
        assertTrue(nextExecuted);
    }

    @Test
    void execute_noTool_noModification() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = GameTestFactory.createAttack("Tackle", List.of(EnergyType.COLORLESS), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        sut.execute(ctx);

        assertEquals(30, ctx.getBaseDamage());
    }

    @Test
    void execute_nullBaseDamage_handlesGracefully() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        TrainerCard muscleBand = new TrainerCard();
        muscleBand.setId("mb-1");
        muscleBand.setName("Muscle Band");
        muscleBand.setSubtype(TrainerSubtype.TOOL);
        muscleBand.setEffect("[\"The attacks of the Pokémon this card is attached to do 20 " +
                "more damage to your opponent's Active Pokémon (before applying Weakness and Resistance).\"]");
        attacker.setTool(muscleBand);

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(GameTestFactory.createPokemonInPlay());
        ctx.setAttackSuccessful(true);

        sut.execute(ctx);
        assertEquals(20, ctx.getBaseDamage());
    }
}
