package ar.edu.utn.frc.tup.piii.engine.combat;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DamageCalculatorTest {

    private DamageCalculator sut;

    @BeforeEach
    void setUp() {
        sut = new DamageCalculator();
    }

    @Test
    void calculateDamage_normalHit_returnsBaseDamage() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.GRASS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Tackle", List.of(EnergyType.COLORLESS), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(30, damage);
    }

    @Test
    void calculateDamage_weaknessHit_returnsDoubleDamage() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.WATER);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "WATER", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Water Blast", List.of(EnergyType.WATER), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(60, damage);
    }

    @Test
    void calculateDamage_resistanceHit_returnsReducedDamage() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIGHTING);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, null, "FIGHTING");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Punch", List.of(EnergyType.FIGHTING), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(10, damage);
    }

    @Test
    void calculateDamage_weaknessAndResistance_appliesBoth() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", "FIRE");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(40, damage);
    }

    @Test
    void calculateDamage_minimumDamage_returnsZero() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIGHTING);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, null, "FIGHTING");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Weak Punch", List.of(EnergyType.FIGHTING), 5);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(0, damage);
    }

    @Test
    void calculateDamage_noBaseDamage_returnsZero() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS);
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.COLORLESS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, null, null);
        ctx.setBaseDamage(null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(0, damage);
    }

    @Test
    void hasMatchingType_unknownTypeString_returnsFalse() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "UNKNOWN_TYPE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(30, damage);
    }

    @Test
    void calculateDamage_hardCharmReducesDamageAfterWeakness() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.WATER);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "WATER", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        TrainerCard hardCharm = new TrainerCard();
        hardCharm.setId("hc-1");
        hardCharm.setName("Hard Charm");
        hardCharm.setSubtype(TrainerSubtype.TOOL);
        hardCharm.setEffect("[\"Any damage done to the Pokémon this card is attached to " +
                "by an opponent's attack is reduced by 20 (after applying Weakness and Resistance).\"]");
        defender.setTool(hardCharm);

        Attack attack = GameTestFactory.createAttack("Water Blast", List.of(EnergyType.WATER), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(40, damage);
    }

    @Test
    void calculateDamage_hardCharmMinZero() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIGHTING);
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.GRASS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        TrainerCard hardCharm = new TrainerCard();
        hardCharm.setId("hc-1");
        hardCharm.setName("Hard Charm");
        hardCharm.setSubtype(TrainerSubtype.TOOL);
        hardCharm.setEffect("[\"Any damage done to the Pokémon this card is attached to " +
                "by an opponent's attack is reduced by 20 (after applying Weakness and Resistance).\"]");
        defender.setTool(hardCharm);

        Attack attack = GameTestFactory.createAttack("Weak Punch", List.of(EnergyType.FIGHTING), 10);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(0, damage);
    }

    @Test
    void calculateDamage_noToolDefender_normalDamage() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.WATER);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "WATER", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Water Blast", List.of(EnergyType.WATER), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(60, damage);
    }

    @Test
    void calculateDamage_shadowCircle_removesWeakness() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        EnergyCard dark = new EnergyCard();
        dark.setEnergyType(EnergyType.DARKNESS);
        dark.setProvidedEnergies(java.util.List.of(EnergyType.DARKNESS));
        defender.getAttachedEnergy().add(dark);

        TrainerCard shadowCircle = new TrainerCard();
        shadowCircle.setId("sc-1");
        shadowCircle.setName("Shadow Circle");
        shadowCircle.setSubtype(ar.edu.utn.frc.tup.piii.enums.TrainerSubtype.STADIUM);
        shadowCircle.setEffect("[\"Each Pokémon that has any Darkness Energy attached to it " +
                "(both yours and your opponent's) has no Weakness.\"]");

        GameBoard board = new GameBoard();
        board.setSharedStadium(shadowCircle);

        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);

        int damage = sut.calculateDamage(ctx);
        assertEquals(30, damage);
    }

    @Test
    void calculateDamage_shadowCircle_noDarknessEnergy_weaknessApplies() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        TrainerCard shadowCircle = new TrainerCard();
        shadowCircle.setId("sc-1");
        shadowCircle.setName("Shadow Circle");
        shadowCircle.setEffect("[\"Each Pokémon that has any Darkness Energy attached to it " +
                "(both yours and your opponent's) has no Weakness.\"]");

        GameBoard board = new GameBoard();
        board.setSharedStadium(shadowCircle);

        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);

        int damage = sut.calculateDamage(ctx);
        assertEquals(60, damage);
    }

    @Test
    void calculateDamage_noStadium_normalWeakness() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);
        assertEquals(60, damage);
    }

    @Test
    void calculateDamage_muscleBandAndHardCharm_combined() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        TrainerCard muscleBand = new TrainerCard();
        muscleBand.setId("mb-1");
        muscleBand.setName("Muscle Band");
        muscleBand.setEffect("[\"The attacks of the Pokémon this card is attached to do 20 " +
                "more damage to your opponent's Active Pokémon (before applying Weakness and Resistance).\"]");
        attacker.setTool(muscleBand);

        TrainerCard hardCharm = new TrainerCard();
        hardCharm.setId("hc-1");
        hardCharm.setName("Hard Charm");
        hardCharm.setEffect("[\"Any damage done to the Pokémon this card is attached to " +
                "by an opponent's attack is reduced by 20 (after applying Weakness and Resistance).\"]");
        defender.setTool(hardCharm);

        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);
        ctx.setBaseDamage(50);

        int damage = sut.calculateDamage(ctx);
        assertEquals(80, damage);
    }

    @Test
    void calculateDamage_emptyCost_noWeaknessOrResistance() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Empty", List.of(), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(30, damage);
    }

    @Test
    void calculateDamage_nullCostList_noWeaknessOrResistance() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("NullCost", List.of(), 30);
        attack.setCost(null);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(30, damage);
    }

    @Test
    void calculateDamage_colorlessOnly_weaknessApplied() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "COLORLESS", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Plain Hit", List.of(EnergyType.COLORLESS), 20);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(40, damage);
    }

    @Test
    void calculateDamage_colorlessOnly_resistanceApplied() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.COLORLESS);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, null, "COLORLESS");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Plain Hit", List.of(EnergyType.COLORLESS), 20);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(0, damage);
    }

    @Test
    void calculateDamage_nullWeakness_skipsWeakness() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, null, "FIRE");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(10, damage);
    }

    @Test
    void calculateDamage_boardPresentButNoStadium_weaknessApplies() {
        // board != null pero sharedStadium == null → rama getSharedStadium()==null.
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, "FIRE", null);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);

        GameBoard board = new GameBoard();
        board.setSharedStadium(null);

        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);

        int damage = sut.calculateDamage(ctx);

        assertEquals(60, damage);
    }

    @Test
    void calculateDamage_resistancePresentButTypeDoesNotMatch_noReduction() {
        // resistance != null pero el tipo de ataque no coincide → rama matches==false en resistencia.
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.FIRE);
        PokemonCard defenderCard = GameTestFactory.createPokemonCardWithWeakness("Defender", 100, null, "WATER");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Fire Spin", List.of(EnergyType.FIRE), 30);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(30, damage);
    }

    @Test
    void calculateDamage_defenderNoTool_normalDamage() {
        PokemonCard attackerCard = GameTestFactory.createBasicPokemonCard("Attacker", 100, EnergyType.WATER);
        PokemonCard defenderCard = GameTestFactory.createBasicPokemonCard("Defender", 100, EnergyType.GRASS);
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay(attackerCard);
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay(defenderCard);
        Attack attack = GameTestFactory.createAttack("Water Gun", List.of(EnergyType.WATER), 20);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, null);

        int damage = sut.calculateDamage(ctx);

        assertEquals(20, damage);
    }
}
