package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InkayEvolveEffectTest {

    private InkayEvolveEffect effect;
    private GameEventPublisher publisher;
    private PlayerBoard board;

    @BeforeEach
    void setUp() {
        effect = new InkayEvolveEffect();
        publisher = mock(GameEventPublisher.class);
        board = new PlayerBoard();
        board.setPlayerId("player-1");
    }

    private PokemonCard pokemon(String name, int hp, PokemonStage stage, String evolvesFrom) {
        PokemonCard card = new PokemonCard();
        card.setId(name);
        card.setName(name);
        card.setHp(hp);
        card.setStage(stage);
        card.setEvolvesFrom(evolvesFrom);
        return card;
    }

    private AbilityEffectContext ctx(PokemonInPlay source) {
        return new AbilityEffectContext(
                "match-1", "player-1", source, null, board, new PlayerBoard(),
                publisher, TurnPhase.ATTACK);
    }

    // ---- canApply ----

    @Test
    void canApply_falseWhenContextNull() {
        assertFalse(effect.canApply(null));
    }

    @Test
    void canApply_falseWhenSourcePokemonNull() {
        AbilityEffectContext ctx = new AbilityEffectContext(
                "m", "p", null, null, board, new PlayerBoard(), publisher, TurnPhase.ATTACK);
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_falseWhenNotConfused() {
        PokemonInPlay inkay = new PokemonInPlay(pokemon("Inkay", 50, PokemonStage.BASIC, null));
        assertFalse(effect.canApply(ctx(inkay)));
    }

    @Test
    void canApply_trueWhenConfused() {
        PokemonInPlay inkay = new PokemonInPlay(pokemon("Inkay", 50, PokemonStage.BASIC, null));
        inkay.getSpecialConditions().add(SpecialCondition.CONFUSED);
        assertTrue(effect.canApply(ctx(inkay)));
    }

    // ---- apply ----

    @Test
    void apply_evolvesAndCarriesDamageAndClearsConditions() {
        PokemonInPlay inkay = new PokemonInPlay(pokemon("Inkay", 50, PokemonStage.BASIC, null));
        inkay.setCurrentHp(30); // 20 de daño recibido
        inkay.getSpecialConditions().add(SpecialCondition.CONFUSED);
        inkay.getSpecialConditions().add(SpecialCondition.POISONED);

        PokemonCard malamar = pokemon("Malamar", 90, PokemonStage.STAGE1, "Inkay");
        board.getDeck().add(malamar);
        board.getDeck().add(GameTestEnergy.grass());

        effect.apply(ctx(inkay));

        assertSame(malamar, inkay.getCard());
        assertEquals(70, inkay.getCurrentHp()); // 90 - 20
        assertTrue(inkay.getSpecialConditions().isEmpty());
        assertFalse(board.getDeck().contains(malamar));
        assertEquals(1, board.getDiscardPile().size());
        verify(publisher).publish(eq(GameEventType.POKEMON_EVOLVED), eq("match-1"), eq("player-1"), any());
    }

    @Test
    void apply_noEvolutionInDeck_publishesNotificationAndDoesNotMutate() {
        PokemonInPlay inkay = new PokemonInPlay(pokemon("Inkay", 50, PokemonStage.BASIC, null));
        inkay.getSpecialConditions().add(SpecialCondition.CONFUSED);
        PokemonCard base = inkay.getCard();

        effect.apply(ctx(inkay));

        assertSame(base, inkay.getCard());
        assertTrue(board.getDiscardPile().isEmpty());
        assertTrue(inkay.getSpecialConditions().contains(SpecialCondition.CONFUSED));
        verify(publisher).publish(eq(GameEventType.NOTIFICATION), eq("match-1"), eq("player-1"), any());
    }

    @Test
    void apply_noEvolution_withNullPublisher_doesNotThrow() {
        PokemonInPlay inkay = new PokemonInPlay(pokemon("Inkay", 50, PokemonStage.BASIC, null));
        AbilityEffectContext ctx = new AbilityEffectContext(
                "m", "p", inkay, null, board, new PlayerBoard(), null, TurnPhase.ATTACK);
        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void apply_evolution_withNullPublisher_stillEvolves() {
        PokemonInPlay inkay = new PokemonInPlay(pokemon("Inkay", 50, PokemonStage.BASIC, null));
        PokemonCard malamar = pokemon("Malamar", 90, PokemonStage.STAGE1, "Inkay");
        board.getDeck().add(malamar);
        AbilityEffectContext ctx = new AbilityEffectContext(
                "m", "p", inkay, null, board, new PlayerBoard(), null, TurnPhase.ATTACK);

        effect.apply(ctx);

        assertSame(malamar, inkay.getCard());
    }

    @Test
    void apply_ignoresNonMatchingEvolutionAndNonPokemonCards() {
        PokemonInPlay inkay = new PokemonInPlay(pokemon("Inkay", 50, PokemonStage.BASIC, null));
        inkay.getSpecialConditions().add(SpecialCondition.CONFUSED);
        // Evolución que NO viene de Inkay + una carta no-Pokémon → no debe evolucionar.
        board.getDeck().add(pokemon("Octillery", 90, PokemonStage.STAGE1, "Remoraid"));
        board.getDeck().add(GameTestEnergy.grass());

        effect.apply(ctx(inkay));

        assertEquals("Inkay", inkay.getCard().getName());
        verify(publisher).publish(eq(GameEventType.NOTIFICATION), any(), any(), any());
    }

    /** Helper local para una carta no-Pokémon en el mazo. */
    static final class GameTestEnergy {
        static GameCard grass() {
            EnergyCard e = new EnergyCard();
            e.setId("e1");
            e.setName("Grass Energy");
            e.setEnergyType(EnergyType.GRASS);
            return e;
        }
    }
}
