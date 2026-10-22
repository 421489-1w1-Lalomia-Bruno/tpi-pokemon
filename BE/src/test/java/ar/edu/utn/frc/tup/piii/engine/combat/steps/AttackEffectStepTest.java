package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.*;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackEffectStepTest {

    private static final String P1_ID = "p1";
    private static final String P2_ID = "p2";

    private AttackEffectStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        sut = new AttackEffectStep();
        nextExecuted = false;
        sut.setNext(new AttackStep() {
            @Override public void execute(AttackContext context) { nextExecuted = true; }
            @Override public void setNext(AttackStep next) {}
        });
    }

    private AttackContext createContext() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10, null);
        GameBoard board = GameTestFactory.createGameBoard(P1_ID, attacker, P2_ID, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);
        return ctx;
    }

    // ========== execute() flow control ==========

    @Test
    void execute_whenNotSuccessful_returns() {
        AttackContext ctx = createContext();
        ctx.setAttackSuccessful(false);

        sut.execute(ctx);

        assertFalse(nextExecuted);
    }

    @Test
    void execute_normalDamage_appliesDamage() {
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        int initialHp = defender.getCurrentHp();
        AttackContext ctx = createContext();
        ctx.setDefender(defender);
        ctx.setFinalDamage(30);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(initialHp - 30, defender.getCurrentHp());
    }

    @Test
    void execute_withDefenderPrevention_reducesDamage() {
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        AttackContext ctx = createContext();
        ctx.setDefender(defender);
        ctx.setFinalDamage(50);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_normalDamage_noDefender_doesNotCrash() {
        AttackContext ctx = createContext();
        ctx.setDefender(null);
        ctx.setFinalDamage(30);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    // ========== execute() effects flow ==========

    @Test
    void execute_drawEffect_drawsCards() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Draw 2 cards.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIRE));
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d2", EnergyType.WATER));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(2, p1Board.getHand().size());
    }

    @Test
    void execute_healEffect_healsAttacker() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Heal 30.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.takeDamage(50);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    // ========== executeOne(): switch cases ==========

    @Test
    void executeOne_switchEffect_appliesDamage() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Deal 10 damage.");
        GameBoard board = GameTestFactory.createGameBoard(P1_ID, attacker, P2_ID, defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_discardFromDeck_withDamageBonus_appliesBonus() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Test", List.of(EnergyType.FIGHTING), 10,
                "Descarta la carta superior del mazo. Si es Energía Fighting, +20.");
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIGHTING));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_deckPeekSelf_doesNotCrash() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Mira las 3 cartas superiores de tu mazo.");
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIRE));
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d2", EnergyType.WATER));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_shuffleDeck_doesNotCrash() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Baraja tu mazo.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_selfBenchDamage_doesNotCrash() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "10 daño a cada Pokémon de tu Banca.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonInPlay bench1 = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActiveAndBench(P1_ID, attacker, List.of(bench1));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_moveEnergy_doesNotCrash() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Mueve 1 Energía de este Pokémon a tu Banca.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonInPlay bench1 = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActiveAndBench(P1_ID, attacker, List.of(bench1));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_selfDamage_appliesDamage() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Este Pokémon se hace 20 de daño.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(attacker.getCurrentHp() < 100);
    }

    @Test
    void executeOne_addDamage_withCondition_appliesOnMatch() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "If heads, this attack does 30 more damage.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);
        ctx.setConditionalFlipHeads(true);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_removeCondition_clearsSpecialConditions() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Remove any special condition from the Defending Pokemon.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        defender.getSpecialConditions().add(SpecialCondition.PARALYZED);
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_discardOpponent_removesCards() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Descarta 1 carta de la mano rival.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        p2Board.getHand().add(GameTestFactory.createBasicEnergyCard("c1", EnergyType.FIRE));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(p2Board.getHand().isEmpty());
    }

    @Test
    void executeOne_recycleFromDiscard_bringsBackCards() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Put 1 card from your discard into your hand.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PokemonCard pokeCard = GameTestFactory.createBasicPokemonCard();
        p1Board.getDiscardPile().add(pokeCard);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertFalse(p1Board.getHand().isEmpty());
    }

    @Test
    void executeOne_shuffleHandIntoDeck_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Mezcla tu mano en tu mazo.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getHand().add(GameTestFactory.createBasicEnergyCard("c1", EnergyType.FIRE));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(p1Board.getHand().isEmpty());
    }

    @Test
    void executeOne_damageBench_appliesToBench() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "10 daño a 1 de la Banca rival.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonInPlay bench1 = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActiveAndBench(P2_ID, defender, List.of(bench1));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(bench1.getCurrentHp() < 100);
    }

    @Test
    void executeOne_energyAttachFromDeck_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Une 1 Energía de tu mazo a este Pokémon.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(1, attacker.getAttachedEnergy().size());
    }

    @Test
    void executeOne_energyAttachFromDiscard_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Une 1 Energía de tu descarte a este Pokémon.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDiscardPile().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(1, attacker.getAttachedEnergy().size());
    }

    @Test
    void executeOne_discardAllEnergySelf_clearsEnergy() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Descarta toda la Energía de este Pokémon.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(attacker.getAttachedEnergy().isEmpty());
    }

    @Test
    void executeOne_discardEnergySelf_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Descarta una Energía de este Pokémon.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_damageBenchSplash_appliesSplashDamage() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "30 daño a 2 de la Banca rival.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonInPlay bench1 = GameTestFactory.createPokemonInPlay();
        PokemonInPlay bench2 = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActiveAndBench(P2_ID, defender, List.of(bench1, bench2));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_deckPeekOpponent_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Mira la carta superior del mazo rival.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        p2Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIRE));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_insteadOfDamageFlow_drawsCardsWithoutDamage() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Double Draw: Draw 2 cards.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIRE));
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d2", EnergyType.WATER));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(2, p1Board.getHand().size());
    }

    @Test
    void executeOne_drawThenShuffle_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Shuffle your hand into your deck, then draw 2 cards.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getHand().add(GameTestFactory.createBasicEnergyCard("c1", EnergyType.FIRE));
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.WATER));
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d2", EnergyType.GRASS));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(2, p1Board.getHand().size());
    }

    @Test
    void executeOne_searchEffect_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Search your deck for 1 card.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIRE));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_flipCoinStatus_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Flip a coin. If heads, the Defending Pokemon is now Paralyzed.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_withEffectBoth_appliesConditionToBoth() {
        Attack attack = new Attack("Confusion Wave", List.of(EnergyType.PSYCHIC), 10,
                "Both Active Pokemon are now Confused.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);
        ctx.setEffectsToApply(List.of(SpecialCondition.CONFUSED));

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(defender.getSpecialConditions().contains(SpecialCondition.CONFUSED));
        assertTrue(attacker.getSpecialConditions().contains(SpecialCondition.CONFUSED));
    }

    @Test
    void executeOne_discardFromDeckWithAttach_works() {
        Attack attack = new Attack("Dig Out", List.of(EnergyType.FIGHTING), 10,
                "Descarta la carta superior del mazo. Si es Fighting, únela a este Pokémon.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIGHTING));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertTrue(attacker.getAttachedEnergy().stream()
                .anyMatch(e -> e.getEnergyType() == EnergyType.FIGHTING));
    }

    @Test
    void execute_withSpecialConditionsAndNoFlip_appliesDirectConditions() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "The Defending Pokemon is now Poisoned and Burned.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(10);
        ctx.setEffectsToApply(List.of(SpecialCondition.POISONED, SpecialCondition.BURNED));

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_multiCoin_doesNotCrash() {
        Attack attack = new Attack("Seafaring", List.of(EnergyType.WATER), 10,
                "Hasta 3 monedas: por cada cara, une 1 Energía Water del descarte a tu Banca.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonInPlay bench1 = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActiveAndBench(P1_ID, attacker, List.of(bench1));
        p1Board.getDiscardPile().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.WATER));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_deckPeekSelf_works() {
        Attack attack = new Attack("Clairvoyant Eye", List.of(EnergyType.COLORLESS), 10,
                "Mira las 3 cartas superiores de tu mazo.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIRE));
        p1Board.getDeck().add(GameTestFactory.createBasicEnergyCard("d2", EnergyType.WATER));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_withDefenderPokeBody_reducesDamage() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonCard defCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.METAL);
        Ability pokeBody = new Ability("Barrier", AbilityType.POKE_BODY,
                "Prevent 60 or less damage done to this Pokemon.");
        defCard.setAbilities(List.of(pokeBody));
        PokemonInPlay defender = new PokemonInPlay(defCard);
        Attack attack = new Attack("Test", List.of(EnergyType.FIGHTING), 10, null);
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);
        ctx.setFinalDamage(50);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertEquals(100, defender.getCurrentHp());
    }

    @Test
    void executeOne_switchSelf_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "Cambia este Pokémon por uno de tu Banca.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonInPlay bench1 = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActiveAndBench(P1_ID, attacker, List.of(bench1));
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void executeOne_switchOpponent_works() {
        Attack attack = new Attack("Test", List.of(EnergyType.COLORLESS), 10,
                "El rival cambia su Activo por uno de su Banca.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PokemonInPlay oppBench = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActiveAndBench(P2_ID, defender, List.of(oppBench));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }
}
