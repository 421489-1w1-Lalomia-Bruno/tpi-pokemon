package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SwitchActiveEffectTest {

    private PlayerBoard playerBoard;
    private GameBoard gameBoard;
    private GameEventPublisher eventPublisher;
    private PokemonInPlay active;
    private PokemonInPlay bench1;

    @BeforeEach
    void setUp() {
        playerBoard = new PlayerBoard();
        playerBoard.setPlayerId("player-1");

        PlayerBoard opponentBoard = new PlayerBoard();
        opponentBoard.setPlayerId("player-2");

        gameBoard = new GameBoard();
        gameBoard.setPlayer1Board(playerBoard);
        gameBoard.setPlayer2Board(opponentBoard);

        eventPublisher = mock(GameEventPublisher.class);

        PokemonCard activeCard = new PokemonCard();
        activeCard.setId("poke-1");
        activeCard.setName("Charizard");
        activeCard.setHp(120);
        active = new PokemonInPlay(activeCard);
        active.setActive(true);
        active.setCurrentHp(80);
        playerBoard.setActivePokemon(active);

        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("poke-2");
        benchCard.setName("Pikachu");
        benchCard.setHp(60);
        bench1 = new PokemonInPlay(benchCard);
        bench1.setActive(false);
        bench1.setCurrentHp(60);
        playerBoard.getBench().add(bench1);
    }

    private TrainerEffectContext buildContext(TrainerCard card, String targetId) {
        return new TrainerEffectContext(gameBoard, "player-1", "match-1", card, targetId, eventPublisher);
    }

    @Test
    void fromRule_switchPattern_parsesCorrectly() {
        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        assertNotNull(effect);
    }

    @Test
    void fromRule_noMatch_returnsNull() {
        assertNull(SwitchActiveEffect.fromRule("Draw 3 cards."));
        assertNull(SwitchActiveEffect.fromRule(null));
    }

    @Test
    void apply_swapsActiveWithFirstBench() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Switch");
        TrainerEffectContext ctx = buildContext(trainer, null);

        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        effect.apply(ctx);

        assertTrue(bench1.isActive());
        assertFalse(active.isActive());
        assertEquals(bench1, playerBoard.getActivePokemon());
        assertTrue(playerBoard.getBench().contains(active));
    }

    @Test
    void apply_withTargetId_swapsSpecificBench() {
        PokemonCard bench2Card = new PokemonCard();
        bench2Card.setId("poke-3");
        bench2Card.setName("Raichu");
        PokemonInPlay bench2 = new PokemonInPlay(bench2Card);
        bench2.setActive(false);
        playerBoard.getBench().add(bench2);

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Switch");
        TrainerEffectContext ctx = buildContext(trainer, "poke-3");

        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        effect.apply(ctx);

        assertEquals(bench2, playerBoard.getActivePokemon());
        assertTrue(playerBoard.getBench().contains(active));
    }

    @Test
    void apply_clearsSpecialConditionsOnBoth() {
        active.getSpecialConditions().add(ar.edu.utn.frc.tup.piii.enums.SpecialCondition.ASLEEP);
        bench1.getSpecialConditions().add(ar.edu.utn.frc.tup.piii.enums.SpecialCondition.POISONED);

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer, null);

        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        effect.apply(ctx);

        assertTrue(active.getSpecialConditions().isEmpty());
        assertTrue(bench1.getSpecialConditions().isEmpty());
    }

    @Test
    void canApply_trueWhenActiveAndBenchExist() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer, null);

        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_falseWhenNoBench() {
        playerBoard.getBench().clear();
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer, null);

        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_falseWhenNoActive() {
        playerBoard.setActivePokemon(null);
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer, null);

        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void apply_publishesEvent() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Switch");
        TrainerEffectContext ctx = buildContext(trainer, null);

        SwitchActiveEffect effect = SwitchActiveEffect.fromRule("Switch your active Pokemon with 1 of your Benched Pokemon.");
        effect.apply(ctx);

        verify(eventPublisher).publish(any(), eq("match-1"), eq("player-1"), anyString());
    }
}
