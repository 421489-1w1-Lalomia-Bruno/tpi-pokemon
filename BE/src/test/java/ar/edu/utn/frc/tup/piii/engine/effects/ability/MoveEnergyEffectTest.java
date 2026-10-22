package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoveEnergyEffectTest {

    private AbilityEffectContext ctx;
    private PlayerBoard sourceBoard;
    private GameEventPublisher publisher;
    private PokemonInPlay sourcePokemon;
    private PokemonInPlay targetPokemon;
    private PokemonInPlay activePokemon;

    @BeforeEach
    void setUp() {
        publisher = mock(GameEventPublisher.class);
        PokemonCard srcCard = new PokemonCard(); srcCard.setId("src"); srcCard.setName("Source");
        sourcePokemon = new PokemonInPlay(srcCard);
        PokemonCard tgtCard = new PokemonCard(); tgtCard.setId("tgt"); tgtCard.setName("Target");
        targetPokemon = new PokemonInPlay(tgtCard);
        PokemonCard actCard = new PokemonCard(); actCard.setId("act"); actCard.setName("Active");
        activePokemon = new PokemonInPlay(actCard);

        sourceBoard = new PlayerBoard();
        sourceBoard.setActivePokemon(activePokemon);
        sourceBoard.getBench().add(sourcePokemon);
        sourceBoard.getBench().add(targetPokemon);

        ctx = new AbilityEffectContext("m", "p", activePokemon, new GameBoard(),
                sourceBoard, new PlayerBoard(), publisher, TurnPhase.ACTIONS);
    }

    @Test
    void canApply_sourceTargetFoundAndHasEnergy_returnsTrue() {
        ctx.setSourceTargetId("src");
        ctx.setTargetId("tgt");
        EnergyCard energy = new EnergyCard();
        energy.setEnergyType(EnergyType.FIRE);
        sourcePokemon.getAttachedEnergy().add(energy);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_sourceBoardNull_returnsFalse() {
        AbilityEffectContext nullBoard = new AbilityEffectContext("m", "p", activePokemon,
                new GameBoard(), null, new PlayerBoard(), publisher, TurnPhase.ACTIONS);
        nullBoard.setSourceTargetId("src");
        nullBoard.setTargetId("tgt");
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertFalse(effect.canApply(nullBoard));
    }

    @Test
    void canApply_sourceTargetIdNull_returnsFalse() {
        ctx.setTargetId("tgt");
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_targetIdNull_returnsFalse() {
        ctx.setSourceTargetId("src");
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_sourcePokemonNotFound_returnsFalse() {
        ctx.setSourceTargetId("nonexistent");
        ctx.setTargetId("tgt");
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_targetPokemonNotFound_returnsFalse() {
        ctx.setSourceTargetId("src");
        ctx.setTargetId("nonexistent");
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_noMatchingEnergy_returnsFalse() {
        ctx.setSourceTargetId("src");
        ctx.setTargetId("tgt");
        EnergyCard energy = new EnergyCard();
        energy.setEnergyType(EnergyType.WATER);
        sourcePokemon.getAttachedEnergy().add(energy);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_usesProvidedEnergies() {
        ctx.setSourceTargetId("src");
        ctx.setTargetId("tgt");
        EnergyCard dce = new EnergyCard();
        dce.setEnergyType(EnergyType.COLORLESS);
        dce.setProvidedEnergies(List.of(EnergyType.FIRE, EnergyType.WATER));
        sourcePokemon.getAttachedEnergy().add(dce);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void apply_movesEnergyToTarget() {
        ctx.setSourceTargetId("src");
        ctx.setTargetId("tgt");
        EnergyCard energy = new EnergyCard();
        energy.setEnergyType(EnergyType.FIRE);
        sourcePokemon.getAttachedEnergy().add(energy);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        effect.apply(ctx);

        assertFalse(sourcePokemon.getAttachedEnergy().contains(energy));
        assertTrue(targetPokemon.getAttachedEnergy().contains(energy));
        verify(publisher).publish(any(), eq("m"), eq("p"), anyString());
    }

    @Test
    void apply_nullPublisher_doesNotThrow() {
        AbilityEffectContext noPub = new AbilityEffectContext("m", "p", activePokemon,
                new GameBoard(), sourceBoard, new PlayerBoard(), null, TurnPhase.ACTIONS);
        noPub.setSourceTargetId("src");
        noPub.setTargetId("tgt");
        EnergyCard energy = new EnergyCard();
        energy.setEnergyType(EnergyType.FIRE);
        sourcePokemon.getAttachedEnergy().add(energy);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertDoesNotThrow(() -> effect.apply(noPub));
        assertTrue(targetPokemon.getAttachedEnergy().contains(energy));
    }

    @Test
    void apply_sourceBoardNull_doesNothing() {
        AbilityEffectContext nullBoard = new AbilityEffectContext("m", "p", activePokemon,
                new GameBoard(), null, new PlayerBoard(), publisher, TurnPhase.ACTIONS);
        nullBoard.setSourceTargetId("src");
        nullBoard.setTargetId("tgt");
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertDoesNotThrow(() -> effect.apply(nullBoard));
    }

    @Test
    void apply_sourceTargetIdsNull_doesNothing() {
        EnergyCard energy = new EnergyCard();
        energy.setEnergyType(EnergyType.FIRE);
        sourcePokemon.getAttachedEnergy().add(energy);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void apply_pokemonNotFound_doesNothing() {
        ctx.setSourceTargetId("nonexistent");
        ctx.setTargetId("tgt");
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void apply_usesProvidedEnergiesForMove() {
        ctx.setSourceTargetId("src");
        ctx.setTargetId("tgt");
        EnergyCard dce = new EnergyCard();
        dce.setEnergyType(EnergyType.COLORLESS);
        dce.setProvidedEnergies(List.of(EnergyType.FIRE, EnergyType.WATER));
        sourcePokemon.getAttachedEnergy().add(dce);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        effect.apply(ctx);

        assertFalse(sourcePokemon.getAttachedEnergy().contains(dce));
        assertTrue(targetPokemon.getAttachedEnergy().contains(dce));
    }

    @Test
    void apply_findPokemonOnActive() {
        PokemonCard actCard = new PokemonCard(); actCard.setId("active-src"); actCard.setName("ActiveSrc");
        PokemonInPlay activeSrc = new PokemonInPlay(actCard);
        sourceBoard.setActivePokemon(activeSrc);
        sourceBoard.getBench().clear();

        ctx.setSourceTargetId("active-src");
        ctx.setTargetId("tgt");
        sourceBoard.getBench().add(targetPokemon);
        EnergyCard energy = new EnergyCard();
        energy.setEnergyType(EnergyType.FIRE);
        activeSrc.getAttachedEnergy().add(energy);
        MoveEnergyEffect effect = new MoveEnergyEffect(EnergyType.FIRE);

        assertTrue(effect.canApply(ctx));
        effect.apply(ctx);
        assertTrue(targetPokemon.getAttachedEnergy().contains(energy));
    }
}
