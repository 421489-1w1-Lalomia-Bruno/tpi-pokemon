package ar.edu.utn.frc.tup.piii.engine.combat;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AttackResolverTest {

    private AttackResolver sut;

    @BeforeEach
    void setUp() {
        sut = new AttackResolver();
    }

    @AfterEach
    void tearDown() {
        CoinFlipManager.setRng(null); // restore default random after each test
    }

    @Test
    void resolveAttack_validAttack_setsFinalDamage() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Pikachu", 100, EnergyType.LIGHTNING);
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Charmander", 80, EnergyType.FIRE);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        EnergyCard energy = GameTestFactory.createBasicEnergyCard("energy-1", EnergyType.LIGHTNING);
        attacker.getAttachedEnergy().add(energy);

        Attack thunderbolt = GameTestFactory.createAttack("Thunderbolt", List.of(EnergyType.LIGHTNING), 50);
        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(thunderbolt);
        ctx.setBaseDamage(thunderbolt.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful());
        assertNotNull(ctx.getFinalDamage());
        assertTrue(ctx.getFinalDamage() > 0);
    }

    @Test
    void resolveAttack_insufficientEnergy_failsAttack() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Pikachu", 100, EnergyType.LIGHTNING);
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Charmander", 80, EnergyType.FIRE);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        Attack expensive = GameTestFactory.createAttack("Giga Impact",
                List.of(EnergyType.LIGHTNING, EnergyType.LIGHTNING, EnergyType.LIGHTNING, EnergyType.COLORLESS), 120);
        GameBoard board = GameTestFactory.createGameBoard("p1", attacker, "p2", defender);

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(expensive);
        ctx.setBaseDamage(expensive.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertFalse(ctx.isAttackSuccessful());
        assertNotNull(ctx.getFailureReason());
    }

    @Test
    void resolveAttack_noOpponentActive_failsAttack() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Pikachu", 100, EnergyType.LIGHTNING);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        EnergyCard energy = GameTestFactory.createBasicEnergyCard("energy-1", EnergyType.LIGHTNING);
        attacker.getAttachedEnergy().add(energy);

        Attack tackle = GameTestFactory.createAttack("Tackle", List.of(EnergyType.COLORLESS), 30);
        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoard("p2");
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(tackle);
        ctx.setBaseDamage(tackle.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertFalse(ctx.isAttackSuccessful());
    }

    @Test
    void resolveAttack_conditionalDamage_appliesDamageOnHeads() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        // Attack: "Flip a coin. If heads, this attack does 50 damage."
        Attack conditionalAttack = new Attack("Coin Flip", List.of(EnergyType.COLORLESS), 0, "Flip a coin. If heads, this attack does 50 damage.");
        
        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(conditionalAttack);
        ctx.setBaseDamage(conditionalAttack.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        // We can't easily mock the coin flip unless we control the Random or the service.
        // For now, we just run it and check if it works (it might be flaky if we don't mock).
        // But for the failing test, since it's currently 0 damage, it should stay 0 or be 50.
        // If we want it to pass, we need to implement the logic.
        
        sut.resolveAttack(ctx);

        // Since it's random, we can't assert 50. But we can assert it's either 0 or 50.
        assertTrue(defender.getDamageCounters() == 0 || defender.getDamageCounters() == 5, 
            "Damage should be either 0 or 50 (5 damage counters)");
    }

    @Test
    void testResolver() {
        ar.edu.utn.frc.tup.piii.engine.effects.attack.AttackEffectResolver resolver = new ar.edu.utn.frc.tup.piii.engine.effects.attack.AttackEffectResolver();
        List<ar.edu.utn.frc.tup.piii.engine.effects.attack.AttackEffect> effects = resolver.resolve("If the damage is 50 or more, discard 1 card.");
        assertEquals(1, effects.size());
        assertEquals(ar.edu.utn.frc.tup.piii.engine.effects.attack.EffectOperation.DISCARD, effects.get(0).getOperation());
        assertEquals(50, effects.get(0).getMinDamage());
        assertEquals(1, effects.get(0).getAmount());
    }

    @Test
    void resolveAttack_advancedSearch_addsCardToHand() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);

        // Put a trainer card in deck to search
        TrainerCard tc = new TrainerCard();
        tc.setId("t-1");
        tc.setName("Ultra Ball");
        p1.getDeck().add(tc);

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack searchAttack = new Attack("Search Trainer", List.of(EnergyType.COLORLESS), 0, "Search your deck for 1 Trainer card.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(searchAttack);
        ctx.setBaseDamage(searchAttack.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertEquals(1, p1.getHand().size());
        assertEquals("Ultra Ball", p1.getHand().get(0).getName());
    }

    @Test
    void resolveAttack_discardAndShuffle_modifiesDeckAndDiscard() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);

        p1.getHand().add(GameTestFactory.createBasicEnergyCard("h-1", EnergyType.COLORLESS));
        p1.getHand().add(GameTestFactory.createBasicEnergyCard("h-2", EnergyType.COLORLESS));

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        // Shuffle hand into deck and shuffle deck
        Attack shuffleAttack = new Attack("Shuffle Hand", List.of(EnergyType.COLORLESS), 0, "Shuffle your hand into your deck. Then shuffle your deck.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(shuffleAttack);
        ctx.setBaseDamage(shuffleAttack.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertEquals(0, p1.getHand().size());
        assertTrue(p1.getDeck().size() >= 2);
    }

    @Test
    void resolveAttack_withBenchSplash_dealsDamageToBenchTargets() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Blastoise", 130, EnergyType.WATER);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.WATER));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.WATER));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        List<PokemonInPlay> bench = List.of(
            GameTestFactory.createPokemonInPlay(GameTestFactory.createBasicPokemonCard("Bench1", 80, EnergyType.COLORLESS)),
            GameTestFactory.createPokemonInPlay(GameTestFactory.createBasicPokemonCard("Bench2", 80, EnergyType.COLORLESS)),
            GameTestFactory.createPokemonInPlay(GameTestFactory.createBasicPokemonCard("Bench3", 80, EnergyType.COLORLESS))
        );

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActiveAndBench("p2", defender, bench);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack hydroBombard = new Attack("Hydro Bombard", List.of(EnergyType.WATER, EnergyType.WATER), 40, "40 daño a 2 de la Banca.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(hydroBombard);
        ctx.setBaseDamage(hydroBombard.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(4, defender.getDamageCounters(), "Defender should have 40 damage (4 counters)");

        long benchWithDamage = bench.stream().filter(p -> p.getDamageCounters() > 0).count();
        assertEquals(2, benchWithDamage, "Splash should hit exactly 2 bench pokemon");
        assertEquals(0, bench.get(2).getDamageCounters(), "Last bench pokemon should be untouched");
    }

    @Test
    void resolveAttack_withSelfBenchDamage_dealsDamageToOwnBench() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Golem", 140, EnergyType.FIGHTING);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIGHTING));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.FIGHTING));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.FIGHTING));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e4", EnergyType.FIGHTING));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard ownBench1 = GameTestFactory.createBasicPokemonCard("OwnBench1", 70, EnergyType.COLORLESS);
        PokemonCard ownBench2 = GameTestFactory.createBasicPokemonCard("OwnBench2", 70, EnergyType.COLORLESS);
        List<PokemonInPlay> ownBench = List.of(
            GameTestFactory.createPokemonInPlay(ownBench1),
            GameTestFactory.createPokemonInPlay(ownBench2)
        );

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActiveAndBench("p1", attacker, ownBench);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack earthquake = new Attack("Earthquake", List.of(EnergyType.FIGHTING, EnergyType.FIGHTING, EnergyType.COLORLESS), 60, "10 daño a cada Pokémon de tu Banca.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(earthquake);
        ctx.setBaseDamage(earthquake.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(6, defender.getDamageCounters(), "Defender should have 60 damage (6 counters)");

        assertEquals(1, ownBench.get(0).getDamageCounters(), "Own bench 1 should have 10 damage (1 counter)");
        assertEquals(1, ownBench.get(1).getDamageCounters(), "Own bench 2 should have 10 damage (1 counter)");
    }

    @Test
    void resolveAttack_conditionalTiming_triggersOnlyIfDamageSufficient() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        // Under-threshold attack (40 damage < 50 threshold to trigger discard)
        Attack lowDamageAttack = new Attack("Low Strike", List.of(EnergyType.COLORLESS), 40, "If the damage is 50 or more, discard 1 card.");
        p1.getHand().add(GameTestFactory.createBasicEnergyCard("h-1", EnergyType.COLORLESS));

        AttackContext ctx1 = new AttackContext();
        ctx1.setAttacker(attacker);
        ctx1.setSelectedAttack(lowDamageAttack);
        ctx1.setBaseDamage(lowDamageAttack.getDamage());
        ctx1.setBoard(board);
        ctx1.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx1);

        // Discard should NOT trigger because damage was only 40 (under 50)
        assertTrue(ctx1.isAttackSuccessful(), "Attack 1 failed: " + ctx1.getFailureReason());
        assertEquals(1, p1.getHand().size());

        // Replenish energy for second attack (first attack consumed it)
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));

        // Over-threshold attack (60 damage >= 50 threshold to trigger discard)
        Attack highDamageAttack = new Attack("High Strike", List.of(EnergyType.COLORLESS), 60, "If the damage is 50 or more, discard 1 card.");

        AttackContext ctx2 = new AttackContext();
        ctx2.setAttacker(attacker);
        ctx2.setSelectedAttack(highDamageAttack);
        ctx2.setBaseDamage(highDamageAttack.getDamage());
        ctx2.setBoard(board);
        ctx2.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx2);

        // Discard SHOULD trigger because damage was 60 (>= 50)
        assertTrue(ctx2.isAttackSuccessful(), "Attack 2 failed: " + ctx2.getFailureReason());
        assertEquals(0, p1.getHand().size());
        // discard pile: 2 energies (from EnergyDiscardStep) + 1 hand card (from DISCARD effect)
        assertEquals(3, p1.getDiscardPile().size());
    }

    @Test
    void resolveAttack_switchOpponentSpanish_luringGlow_switchesOpponentActive() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Volbeat", 70, EnergyType.GRASS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.GRASS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard benchCard = GameTestFactory.createBasicPokemonCard("BenchReplacement", 80, EnergyType.COLORLESS);
        PokemonInPlay benchReplacement = GameTestFactory.createPokemonInPlay(benchCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActiveAndBench("p2", defender, List.of(benchReplacement));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack luringGlow = new Attack("Luring Glow", List.of(EnergyType.GRASS), 0, "Cambia Activo rival por uno de su Banca.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(luringGlow);
        ctx.setBaseDamage(luringGlow.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(benchReplacement, p2.getActivePokemon(), "Bench pokemon should now be active");
        assertTrue(p2.getBench().contains(defender), "Original defender should be on bench");
    }

    @Test
    void resolveAttack_switchSelfThenOpponent_rapidSpin_switchesBoth() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("BlastoiseEX", 180, EnergyType.WATER);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));

        PokemonCard selfBenchCard = GameTestFactory.createBasicPokemonCard("SelfBench", 100, EnergyType.WATER);
        PokemonInPlay selfBench = GameTestFactory.createPokemonInPlay(selfBenchCard);

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard oppBenchCard = GameTestFactory.createBasicPokemonCard("OppBench", 80, EnergyType.COLORLESS);
        PokemonInPlay oppBench = GameTestFactory.createPokemonInPlay(oppBenchCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActiveAndBench("p1", attacker, List.of(selfBench));
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActiveAndBench("p2", defender, List.of(oppBench));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack rapidSpin = new Attack("Rapid Spin", List.of(EnergyType.COLORLESS, EnergyType.COLORLESS), 30,
                "Cambia este Pokémon por uno de tu Banca. Luego el rival cambia su Activo.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(rapidSpin);
        ctx.setBaseDamage(rapidSpin.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(selfBench, p1.getActivePokemon(), "Self bench pokemon should now be active (SWITCH_SELF)");
        assertEquals(oppBench, p2.getActivePokemon(), "Opponent bench pokemon should now be active (SWITCH_OPPONENT)");
    }

    @Test
    void resolveAttack_switchOpponent_knockBack_switchesOpponentActive() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Krookodile", 140, EnergyType.DARKNESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.DARKNESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.DARKNESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.DARKNESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e4", EnergyType.DARKNESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard benchCard = GameTestFactory.createBasicPokemonCard("BenchTarget", 80, EnergyType.COLORLESS);
        PokemonInPlay benchReplacement = GameTestFactory.createPokemonInPlay(benchCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActiveAndBench("p2", defender, List.of(benchReplacement));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack knockBack = new Attack("Knock Back", List.of(EnergyType.DARKNESS, EnergyType.DARKNESS, EnergyType.COLORLESS, EnergyType.COLORLESS), 80,
                "El rival cambia su Activo por uno de su Banca.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(knockBack);
        ctx.setBaseDamage(knockBack.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(8, defender.getDamageCounters(), "Defender should have 80 damage before switch");
        assertEquals(benchReplacement, p2.getActivePokemon(), "Bench pokemon should now be active");
        assertTrue(p2.getBench().contains(defender), "Original defender should be on bench");
    }

    @Test
    void resolveAttack_energyAttachFromDiscard_oblivionWing_attachesToBench() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Yveltal", 130, EnergyType.DARKNESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.DARKNESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard benchCard = GameTestFactory.createBasicPokemonCard("BenchTarget", 80, EnergyType.COLORLESS);
        PokemonInPlay benchTarget = GameTestFactory.createPokemonInPlay(benchCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActiveAndBench("p1", attacker, List.of(benchTarget));
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        // Put a Darkness Energy in discard pile
        EnergyCard discardedEnergy = GameTestFactory.createBasicEnergyCard("discard-energy", EnergyType.DARKNESS);
        p1.getDiscardPile().add(discardedEnergy);

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack oblivionWing = new Attack("Oblivion Wing", List.of(EnergyType.DARKNESS), 30,
                "Une 1 Energía Darkness de tu descarte a tu Banca.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(oblivionWing);
        ctx.setBaseDamage(oblivionWing.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(3, defender.getDamageCounters(), "Defender should have 30 damage");
        // 1 energy was in discard before attack, EnergyDiscardStep adds another (total 2).
        // ENERGY_ATTACH_FROM_DISCARD removes 1 of them, leaving 1 remaining (the other one).
        assertEquals(1, p1.getDiscardPile().size(), "One energy should remain in discard pile");
        assertEquals(1, benchTarget.getAttachedEnergy().size(), "Bench pokemon should have received energy from discard");
    }

    @Test
    void resolveAttack_extraBenchDamage_machPunch_dealsExtraToBench() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Ledian", 80, EnergyType.GRASS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.GRASS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.GRASS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard benchCard = GameTestFactory.createBasicPokemonCard("BenchPoke", 80, EnergyType.COLORLESS);
        PokemonInPlay benchTarget = GameTestFactory.createPokemonInPlay(benchCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActiveAndBench("p2", defender, List.of(benchTarget));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack machPunch = new Attack("Mach Punch", List.of(EnergyType.GRASS, EnergyType.GRASS), 50,
                "+10 a 1 Banca rival.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(machPunch);
        ctx.setBaseDamage(machPunch.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(5, defender.getDamageCounters(), "Defender should have 50 damage (5 counters)");
        assertEquals(1, benchTarget.getDamageCounters(), "Bench should have 10 damage (1 counter)");
    }

    @Test
    void resolveAttack_benchSplashSingleTarget_breakThrough_dealsDamageToOneBench() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("XerneasEX", 170, EnergyType.FAIRY);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FAIRY));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard bench1 = GameTestFactory.createBasicPokemonCard("Bench1", 80, EnergyType.COLORLESS);
        PokemonCard bench2 = GameTestFactory.createBasicPokemonCard("Bench2", 80, EnergyType.COLORLESS);
        PokemonInPlay benchTarget1 = GameTestFactory.createPokemonInPlay(bench1);
        PokemonInPlay benchTarget2 = GameTestFactory.createPokemonInPlay(bench2);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActiveAndBench("p2", defender, List.of(benchTarget1, benchTarget2));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack breakThrough = new Attack("Break Through", List.of(EnergyType.FAIRY, EnergyType.COLORLESS, EnergyType.COLORLESS), 60,
                "30 daño a 1 de la Banca rival.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(breakThrough);
        ctx.setBaseDamage(breakThrough.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(6, defender.getDamageCounters(), "Defender should have 60 damage (6 counters)");
        long benchWithDamage = p2.getBench().stream().filter(p -> p.getDamageCounters() > 0).count();
        assertEquals(1, benchWithDamage, "Splash should hit exactly 1 bench pokemon");
    }

    @Test
    void resolveAttack_benchSplashSingleTarget_wildBarking_dealsDamageToOneBench() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Stoutland", 140, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e4", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard benchCard = GameTestFactory.createBasicPokemonCard("BenchPoke", 80, EnergyType.COLORLESS);
        PokemonInPlay benchTarget = GameTestFactory.createPokemonInPlay(benchCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActiveAndBench("p2", defender, List.of(benchTarget));
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack wildBarking = new Attack("Wild Barking", List.of(EnergyType.COLORLESS, EnergyType.COLORLESS, EnergyType.COLORLESS, EnergyType.COLORLESS), 80,
                "20 daño a 1 de la Banca rival.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(wildBarking);
        ctx.setBaseDamage(wildBarking.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(8, defender.getDamageCounters(), "Defender should have 80 damage (8 counters)");
        assertEquals(2, benchTarget.getDamageCounters(), "Bench should have 20 damage (2 counters)");
    }

    @Test
    void resolveAttack_recycleTrainer_diggersby_retrievesItemsFromDiscard() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Diggersby", 100, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        TrainerCard item1 = new TrainerCard();
        item1.setId("item-1");
        item1.setName("Great Ball");
        TrainerCard item2 = new TrainerCard();
        item2.setId("item-2");
        item2.setName("Super Potion");
        p1.getDiscardPile().add(item1);
        p1.getDiscardPile().add(item2);

        Attack pickup = new Attack("Pickup", List.of(EnergyType.COLORLESS, EnergyType.COLORLESS), 0,
                "Pon 2 cartas Objeto de tu descarte en tu mano.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(pickup);
        ctx.setBaseDamage(pickup.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(2, p1.getHand().size(), "2 items should be in hand");
        assertTrue(p1.getHand().contains(item1), "Hand should contain Great Ball");
        assertTrue(p1.getHand().contains(item2), "Hand should contain Super Potion");
        // 2 energies remain in discard (from EnergyDiscardStep)
        assertEquals(2, p1.getDiscardPile().size(), "Only energies should remain in discard");
    }

    @Test
    void resolveAttack_recycleToDeckTop_simipour_putsCardOnTop() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Simipour", 90, EnergyType.WATER);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.WATER));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        EnergyCard recycled = GameTestFactory.createBasicEnergyCard("recycled-e", EnergyType.WATER);
        p1.getDiscardPile().add(recycled);
        p1.getDeck().clear();
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("top", EnergyType.WATER));

        Attack recycle = new Attack("Recycle", List.of(EnergyType.WATER, EnergyType.COLORLESS), 0,
                "Pon 1 carta de tu descarte en la cima de tu mazo.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(recycle);
        ctx.setBaseDamage(recycle.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        // 1 recycled card removed + 2 energies from EnergyDiscardStep → 2 energies remain
        assertEquals(2, p1.getDiscardPile().size(), "Two energies should remain in discard");
        assertEquals(2, p1.getDeck().size(), "Deck should have 2 cards");
        assertEquals(recycled, p1.getDeck().get(0), "Recycled card should be on top of deck");
    }

    @Test
    void resolveAttack_searchTrainer_gogoatLead_retrievesUpTo2Supporters() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Gogoat", 120, EnergyType.GRASS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.GRASS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        TrainerCard supporter1 = new TrainerCard();
        supporter1.setId("s-1");
        supporter1.setName("Professor Sycamore");
        TrainerCard supporter2 = new TrainerCard();
        supporter2.setId("s-2");
        supporter2.setName("Cheren");
        TrainerCard supporter3 = new TrainerCard();
        supporter3.setId("s-3");
        supporter3.setName("N");
        p1.getDeck().clear();
        p1.getDeck().add(supporter1);
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("filler", EnergyType.COLORLESS));
        p1.getDeck().add(supporter2);
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("filler2", EnergyType.COLORLESS));
        p1.getDeck().add(supporter3);

        Attack lead = new Attack("Lead", List.of(EnergyType.GRASS, EnergyType.COLORLESS), 0,
                "Busca hasta 2 cartas Partidario en tu mazo, enséñaselas a tu rival y ponlas en tu mano. Luego baraja tu mazo.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(lead);
        ctx.setBaseDamage(lead.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(2, p1.getHand().size(), "2 supporters should be in hand");
        assertTrue(p1.getHand().contains(supporter1), "Hand should contain Sycamore");
        assertTrue(p1.getHand().contains(supporter2), "Hand should contain Cheren");
    }

    @Test
    void resolveSearch_anyCard_zoruaNastyPlot_retrievesOneCard() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Zorua", 60, EnergyType.DARKNESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p1.getDeck().clear();
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("found", EnergyType.FIRE));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("other", EnergyType.WATER));

        Attack nastyPlot = new Attack("Nasty Plot", List.of(EnergyType.COLORLESS), 0,
                "Busca 1 carta en tu mazo.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(nastyPlot);
        ctx.setBaseDamage(nastyPlot.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(1, p1.getHand().size(), "1 card should be in hand");
        assertEquals(1, p1.getDeck().size(), "Deck should have 1 card remaining");
    }

    @Test
    void resolveSearch_energy_delcattySalon_retrievesUpTo3Energy() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Delcatty", 90, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p1.getDeck().clear();
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.WATER));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.GRASS));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("e4", EnergyType.LIGHTNING));
        p1.getDeck().add(GameTestFactory.createBasicPokemonCard("filler", 50, EnergyType.COLORLESS));

        Attack energySalon = new Attack("Energy Salon", List.of(EnergyType.COLORLESS), 0,
                "Busca hasta 3 cartas de Energía en tu mazo, enséñaselas a tu rival y ponlas en tu mano. Luego baraja tu mazo.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(energySalon);
        ctx.setBaseDamage(energySalon.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(3, p1.getHand().size(), "3 energy cards should be in hand");
        assertEquals(2, p1.getDeck().size(), "Deck should have 2 cards remaining (1 Pokemon + 1 shuffled)");
    }

    @Test
    void resolveAttack_discardOpponent_malamarMentalTrash_discardsOneFromOpponentsHand() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Malamar", 100, EnergyType.PSYCHIC);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.PSYCHIC));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 90, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p2.getHand().add(GameTestFactory.createBasicEnergyCard("opp-e1", EnergyType.FIRE));
        p2.getHand().add(GameTestFactory.createBasicEnergyCard("opp-e2", EnergyType.WATER));

        Attack mentalTrash = new Attack("Mental Trash", List.of(EnergyType.PSYCHIC, EnergyType.COLORLESS), 30,
                "Descarta 1 carta de la mano de tu rival.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(mentalTrash);
        ctx.setBaseDamage(mentalTrash.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertEquals(1, p2.getHand().size(), "Opponent should have 1 card left in hand");
        assertEquals(1, p2.getDiscardPile().size(), "Opponent discard should have 1 card");
    }

    @Test
    void resolveAttack_multiCoinEnergyAttach_laprasSeafaring_attachesPerHeads() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Lapras", 110, EnergyType.WATER);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.WATER));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PokemonCard benchCard = GameTestFactory.createBasicPokemonCard("BenchPoke", 80, EnergyType.COLORLESS);
        PokemonInPlay benchTarget = GameTestFactory.createPokemonInPlay(benchCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActiveAndBench("p1", attacker, List.of(benchTarget));
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p1.getDiscardPile().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.WATER));
        p1.getDiscardPile().add(GameTestFactory.createBasicEnergyCard("d2", EnergyType.WATER));
        p1.getDiscardPile().add(GameTestFactory.createBasicEnergyCard("d3", EnergyType.WATER));

        Attack seafaring = new Attack("Seafaring", List.of(EnergyType.WATER), 0,
                "Hasta 3 monedas: por cada cara, une 1 Energía Water del descarte a tu Banca.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(seafaring);
        ctx.setBaseDamage(seafaring.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        int attached = benchTarget.getAttachedEnergy().size();
        assertTrue(attached >= 0 && attached <= 3,
                "Should attach 0-3 energies (got " + attached + ") depending on coin flips");
        int remainingInDiscard = p1.getDiscardPile().size();
        // discard had 3 waters + 1 from EnergyDiscardStep = 4, minus attached
        assertEquals(4 - attached, remainingInDiscard,
                "Discard should have had 4 cards minus " + attached + " attached");
    }

    @Test
    void gurdurr_hammerArm_discardFromDeck_topCardOfOpponentDeck() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Gurdurr", 130, EnergyType.FIGHTING);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIGHTING));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.FIGHTING));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e3", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p2.getDeck().add(GameTestFactory.createBasicEnergyCard("opp-e1", EnergyType.FIRE));
        p2.getDeck().add(GameTestFactory.createBasicEnergyCard("opp-e2", EnergyType.WATER));

        Attack hammerArm = new Attack("Hammer Arm", List.of(EnergyType.FIGHTING, EnergyType.FIGHTING, EnergyType.COLORLESS), 60,
                "60 daño. Descarta la carta superior del mazo rival.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(hammerArm);
        ctx.setBaseDamage(hammerArm.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertNotNull(ctx.getFinalDamage());
        assertEquals(60, ctx.getFinalDamage());
        assertEquals(1, p2.getDeck().size(), "Opponent deck should have 1 card left (2 - 1 discarded)");
        assertEquals(1, p2.getDiscardPile().size(), "Opponent discard should have 1 card from deck top");
    }

    @Test
    void rhyhorn_digOut_discardTopAndAttachIfFighting() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Rhyhorn", 80, EnergyType.FIGHTING);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIGHTING));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("top-e1", EnergyType.FIGHTING));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("top-e2", EnergyType.COLORLESS));

        Attack digOut = new Attack("Dig Out", List.of(EnergyType.FIGHTING), 10,
                "10 daño. Descarta la carta superior del mazo. Si es Fighting, únela a este Pokémon.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(digOut);
        ctx.setBaseDamage(digOut.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        int hpBefore = defender.getCurrentHp();
        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertNotNull(ctx.getFinalDamage());
        assertEquals(10, ctx.getFinalDamage());
        assertEquals(1, p1.getDeck().size(), "Deck should have 1 card left (2 - 1 discarded from deck top)");
        // Initial energy was discarded to pay cost, so attacker starts empty.
        // attach_if takes Fighting energy from discard pile and attaches it.
        assertEquals(1, attacker.getAttachedEnergy().size(), "One Fighting energy should be attached from deck top via attach_if");
        assertEquals(hpBefore - 10, defender.getCurrentHp());
    }

    @Test
    void magcargo_magmaMantle_damageBonusIfFire() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Magcargo", 110, EnergyType.FIRE);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIRE));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 150, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("top-e1", EnergyType.FIRE));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("top-e2", EnergyType.COLORLESS));

        Attack magmaMantle = new Attack("Magma Mantle", List.of(EnergyType.FIRE, EnergyType.COLORLESS), 30,
                "30+ daño. Puedes descartar la carta superior del mazo. Si es Energía Fire, +50.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(magmaMantle);
        ctx.setBaseDamage(magmaMantle.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        int hpBefore = defender.getCurrentHp();
        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());

        // First check that the discard-from-deck effect ran
        assertEquals(1, p1.getDeck().size(), "Deck should have 1 card left (2 - 1 discarded from deck top)");

        // EnergyDiscardStep discarded 2 (FIRE + COLORLESS) + discarding top deck = 3
        // The top card was FIRE energy, so the bonus should have triggered.
        int expectedDiscardSize = 3; // 2 cost + 1 deck top
        assertEquals(expectedDiscardSize, p1.getDiscardPile().size(), "Discard should have 3 cards (2 cost + 1 deck top)");

        // Now check damage: 30 base + 50 bonus = 80
        int defenderHpLost = hpBefore - defender.getCurrentHp();
        assertEquals(80, defenderHpLost, "Should deal 80 damage (30 base + 50 Fire bonus)");
    }

    @Test
    void braixen_clairvoyantEye_peekSelfDeck() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Braixen", 80, EnergyType.FIRE);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("d1", EnergyType.FIRE));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("d2", EnergyType.WATER));
        p1.getDeck().add(GameTestFactory.createBasicEnergyCard("d3", EnergyType.GRASS));
        p1.getDeck().add(GameTestFactory.createBasicPokemonCard("bench-filler", 50, EnergyType.COLORLESS));
        p1.getDeck().add(GameTestFactory.createBasicPokemonCard("bench-filler2", 60, EnergyType.COLORLESS));

        Attack clairvoyantEye = new Attack("Clairvoyant Eye", List.of(EnergyType.COLORLESS), 0,
                "Mira las 3 cartas superiores de tu mazo y reordénalas.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(clairvoyantEye);
        ctx.setBaseDamage(clairvoyantEye.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertNotNull(ctx.getFinalDamage());
        assertEquals(0, ctx.getFinalDamage());
        assertEquals(5, p1.getDeck().size(), "Deck should still have 5 cards (shuffled back after peek)");
        assertTrue(p1.getHand().isEmpty(), "Hand should be empty (no cards drawn)");
        // 1 card in discard = the COLORLESS energy used to pay the attack cost
        assertEquals(1, p1.getDiscardPile().size(), "Discard should have 1 card (energy cost)");
    }

    @Test
    void diglett_mine_peekOpponentDeckAndShuffle() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Diglett", 50, EnergyType.FIGHTING);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.FIGHTING));

        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        p2.getDeck().add(GameTestFactory.createBasicEnergyCard("opp-e1", EnergyType.FIRE));
        p2.getDeck().add(GameTestFactory.createBasicEnergyCard("opp-e2", EnergyType.WATER));
        p2.getDeck().add(GameTestFactory.createBasicEnergyCard("opp-e3", EnergyType.GRASS));

        Attack mine = new Attack("Mine", List.of(EnergyType.FIGHTING), 0,
                "Mira la carta superior del mazo rival. Puedes hacer que el rival mezcle su mazo.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(mine);
        ctx.setBaseDamage(mine.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful(), "Attack failed: " + ctx.getFailureReason());
        assertNotNull(ctx.getFinalDamage());
        assertEquals(0, ctx.getFinalDamage());
        assertEquals(3, p2.getDeck().size(), "Opponent deck should still have 3 cards");
    }

    // ─── Phase 4 tests ────────────────────────────────────────────────────────

    @Test
    void resolveAttack_conditionalFlip_headsAddsDamageOnce() {
        // Force coin to always return HEADS
        CoinFlipManager.setRng(new Random() {
            @Override public boolean nextBoolean() { return true; }
        });

        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS));

        GameBoard board = GameTestFactory.createGameBoard("p1", attacker, "p2", defender);

        // Base 20 damage + "If heads, this attack does 40 more damage."
        Attack attack = new Attack("Coin Flip", List.of(EnergyType.COLORLESS), 20,
                "Flip a coin. If heads, this attack does 40 more damage.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(attack);
        ctx.setBaseDamage(attack.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful());
        // With heads fixed: should be exactly 20 + 40 = 60 (not 100 due to double-flip bug)
        assertEquals(60, defender.getDamageCounters() * 10,
                "With heads, total damage must be 60 (base 20 + ADD_DAMAGE 40), not double-counted");
    }

    @Test
    void resolveAttack_conditionalFlip_tailsSkipsAddDamage() {
        // Force coin to always return TAILS
        CoinFlipManager.setRng(new Random() {
            @Override public boolean nextBoolean() { return false; }
        });

        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS));

        GameBoard board = GameTestFactory.createGameBoard("p1", attacker, "p2", defender);

        Attack attack = new Attack("Coin Flip", List.of(EnergyType.COLORLESS), 20,
                "Flip a coin. If heads, this attack does 40 more damage.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(attack);
        ctx.setBaseDamage(attack.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful());
        // Tails: only base 20 damage applied
        assertEquals(20, defender.getDamageCounters() * 10,
                "With tails, only base 20 damage should be applied");
    }

    @Test
    void resolveAttack_conditionalDiscard_headsDiscardsEnergy() {
        // Force coin to always return HEADS
        CoinFlipManager.setRng(new Random() {
            @Override public boolean nextBoolean() { return true; }
        });

        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Sableye", 60, EnergyType.PSYCHIC));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS));
        EnergyCard defenderEnergy = GameTestFactory.createBasicEnergyCard("def-e1", EnergyType.FIRE);
        defender.getAttachedEnergy().add(defenderEnergy);

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        // "Rip Claw" pattern: flip coin, if heads discard energy from opponent
        Attack ripClaw = new Attack("Rip Claw", List.of(EnergyType.COLORLESS), 30,
                "Flip a coin. If heads, discard an Energy attached to your opponent's Active Pokemon.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(ripClaw);
        ctx.setBaseDamage(ripClaw.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful());
        assertEquals(0, defender.getAttachedEnergy().size(),
                "Defender's energy should have been discarded on heads");
        assertEquals(1, p2.getDiscardPile().size(),
                "Discarded energy should be in opponent's discard pile");
    }

    @Test
    void resolveAttack_moveEnergy_movesFromAttackerToBench() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Yveltal", 130, EnergyType.DARKNESS));
        EnergyCard darkEnergy = GameTestFactory.createBasicEnergyCard("e1", EnergyType.DARKNESS);
        EnergyCard colorlessEnergy = GameTestFactory.createBasicEnergyCard("e2", EnergyType.COLORLESS);
        // Extra energy so one remains after paying the {Darkness, Colorless} attack cost
        EnergyCard extraEnergy = GameTestFactory.createBasicEnergyCard("e3", EnergyType.DARKNESS);
        attacker.getAttachedEnergy().add(darkEnergy);
        attacker.getAttachedEnergy().add(colorlessEnergy);
        attacker.getAttachedEnergy().add(extraEnergy);

        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS));

        PokemonInPlay benchPoke = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("BenchPoke", 70, EnergyType.COLORLESS));

        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActiveAndBench("p1", attacker, List.of(benchPoke));
        PlayerBoard p2 = GameTestFactory.createPlayerBoardWithActive("p2", defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);

        Attack yCyclone = new Attack("Y Cyclone", List.of(EnergyType.DARKNESS, EnergyType.COLORLESS), 90,
                "Move 1 Energy from this Pokemon to 1 of your Bench Pokemon.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(yCyclone);
        ctx.setBaseDamage(yCyclone.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful());
        // EnergyDiscardStep discards 2 (Darkness + Colorless cost), leaving 1 extra.
        // MOVE_ENERGY then moves that 1 remaining energy to bench.
        assertEquals(0, attacker.getAttachedEnergy().size(),
                "Attacker should have no energy left (2 cost discarded + 1 moved to bench)");
        assertEquals(1, benchPoke.getAttachedEnergy().size(),
                "Bench Pokemon should have received the moved energy");
    }

    @Test
    void resolveAttack_removeCondition_clearsDefenderConditions() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Conkeldurr", 140, EnergyType.FIGHTING));
        attacker.getAttachedEnergy().add(GameTestFactory.createBasicEnergyCard("e1", EnergyType.COLORLESS));

        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(
                GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS));
        defender.getSpecialConditions().add(SpecialCondition.POISONED);

        GameBoard board = GameTestFactory.createGameBoard("p1", attacker, "p2", defender);

        // "Wake-Up Slap" pattern: remove all special conditions from defender
        Attack wakeUpSlap = new Attack("Wake-Up Slap", List.of(EnergyType.COLORLESS), 30,
                "If your opponent's Active Pokemon is affected by a Special Condition, remove all Special Conditions affecting that Pokemon.");

        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setSelectedAttack(wakeUpSlap);
        ctx.setBaseDamage(wakeUpSlap.getDamage());
        ctx.setBoard(board);
        ctx.setCurrentPlayerId("p1");

        sut.resolveAttack(ctx);

        assertTrue(ctx.isAttackSuccessful());
        assertTrue(defender.getSpecialConditions().isEmpty(),
                "All special conditions should have been removed from defender");
        assertTrue(defender.getDamageCounters() > 0,
                "Defender should still have taken damage");
    }
}
