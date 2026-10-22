package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.models.Ability;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AbilityEffectResolverTest {

    private final AbilityEffectResolver sut = new AbilityEffectResolver();

    @Test
    void resolve_nullAbility_returnsEmpty() {
        List<AbilityEffect> effects = sut.resolve(null);
        assertTrue(effects.isEmpty());
    }

    @Test
    void resolve_nullEffectText_returnsEmpty() {
        Ability ability = new Ability();
        ability.setName("Test");
        ability.setEffectText(null);
        List<AbilityEffect> effects = sut.resolve(ability);
        assertTrue(effects.isEmpty());
    }

    @Test
    void resolve_emptyText_returnsEmpty() {
        Ability ability = new Ability();
        ability.setName("Test");
        ability.setEffectText("");
        List<AbilityEffect> effects = sut.resolve(ability);
        assertTrue(effects.isEmpty());
    }

    @Test
    void resolve_flipCoins_createsFlipCoinsEffect() {
        Ability ability = new Ability();
        ability.setName("Flip Test");
        ability.setEffectText("Flip 3 coins. If tails, this Pokémon does nothing.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertFalse(effects.isEmpty());
        assertTrue(effects.get(0) instanceof FlipCoinsEffect);
        assertEquals(1, effects.size());
    }

    @Test
    void resolve_flipCoinsWithDiscard_createsBoth() {
        Ability ability = new Ability();
        ability.setName("Energy Steal");
        ability.setEffectText("Flip 2 coins. For each tails, discard a card from your opponent's hand.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertTrue(effects.size() >= 2);
        assertTrue(effects.get(0) instanceof FlipCoinsEffect);
        assertTrue(effects.get(1) instanceof DiscardCardsEffect);
    }

    @Test
    void resolve_preventDamage_withoutThreshold() {
        Ability ability = new Ability();
        ability.setName("Prevent Test");
        ability.setEffectText("Prevent all damage done to this Pokémon.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertFalse(effects.isEmpty());
        assertTrue(effects.get(0) instanceof PreventDamageEffect);
        assertEquals(0, ((PreventDamageEffect) effects.get(0)).getThreshold());
        assertFalse(((PreventDamageEffect) effects.get(0)).isOnlyOpponentTurn());
    }

    @Test
    void resolve_preventDamage_withThreshold() {
        Ability ability = new Ability();
        ability.setName("Armor");
        ability.setEffectText("Prevent all damage done to this Pokémon by attacks from Basic Pokémon (30 or less).");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertFalse(effects.isEmpty());
        assertTrue(effects.get(0) instanceof PreventDamageEffect);
        assertEquals(30, ((PreventDamageEffect) effects.get(0)).getThreshold());
    }

    @Test
    void resolve_preventDamage_opponentTurnOnly() {
        Ability ability = new Ability();
        ability.setName("Barrier");
        ability.setEffectText("Prevent all damage done to this Pokémon during your opponent's turn.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertFalse(effects.isEmpty());
        assertTrue(effects.get(0) instanceof PreventDamageEffect);
        assertTrue(((PreventDamageEffect) effects.get(0)).isOnlyOpponentTurn());
    }

    @Test
    void resolve_moveEnergy_createsMoveEnergyEffect() {
        Ability ability = new Ability();
        ability.setName("Energy Shift");
        ability.setEffectText("Move a Fire Energy from this Pokémon to another of your Pokémon.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertFalse(effects.isEmpty());
        assertTrue(effects.get(0) instanceof MoveEnergyEffect);
    }

    @Test
    void resolve_moveEnergy_invalidType_returnsEmpty() {
        Ability ability = new Ability();
        ability.setName("Energy Shift");
        ability.setEffectText("Move a UNKNOWN Energy from this Pokémon to another of your Pokémon.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertTrue(effects.isEmpty());
    }

    @Test
    void resolve_noMatch_returnsEmpty() {
        Ability ability = new Ability();
        ability.setName("No Effect");
        ability.setEffectText("This ability does something unrelated.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertTrue(effects.isEmpty());
    }

    @Test
    void resolve_multipleEffects_allPresent() {
        Ability ability = new Ability();
        ability.setName("Complex");
        ability.setEffectText("Flip 2 coins. For each tails, discard a card from your opponent's hand. " +
                "Prevent all damage done to this Pokémon during your opponent's turn (30 or less). " +
                "Move a Water Energy from this Pokémon to another of your Pokémon.");

        List<AbilityEffect> effects = sut.resolve(ability);

        assertTrue(effects.stream().anyMatch(e -> e instanceof FlipCoinsEffect));
        assertTrue(effects.stream().anyMatch(e -> e instanceof DiscardCardsEffect));
        assertTrue(effects.stream().anyMatch(e -> e instanceof PreventDamageEffect));
        assertTrue(effects.stream().anyMatch(e -> e instanceof MoveEnergyEffect));
    }
}
