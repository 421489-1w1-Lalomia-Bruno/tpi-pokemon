package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameTestFactory {

    public static PokemonCard createBasicPokemonCard(String name, int hp, EnergyType type) {
        PokemonCard card = new PokemonCard();
        card.setId(UUID.randomUUID().toString());
        card.setName(name);
        card.setHp(hp);
        card.setStage(PokemonStage.BASIC);
        card.setTypes(Set.of(type));
        card.setRetreatCost(1);
        return card;
    }

    public static PokemonCard createBasicPokemonCard() {
        return createBasicPokemonCard("TestPokemon", 100, EnergyType.FIRE);
    }

    public static PokemonCard createPokemonCardWithWeakness(String name, int hp, String weakness, String resistance) {
        PokemonCard card = createBasicPokemonCard(name, hp, EnergyType.GRASS);
        card.setWeaknessType(weakness);
        card.setResistanceType(resistance);
        return card;
    }

    public static PokemonInPlay createPokemonInPlay() {
        return new PokemonInPlay(createBasicPokemonCard());
    }

    public static PokemonInPlay createPokemonInPlay(PokemonCard card) {
        return new PokemonInPlay(card);
    }

    public static Attack createAttack(String name, List<EnergyType> cost, int damage) {
        return new Attack(name, cost, damage, "");
    }

    public static EnergyCard createBasicEnergyCard(String id, EnergyType type) {
        EnergyCard energy = new EnergyCard();
        energy.setId(id);
        energy.setName(type.name() + " Energy");
        energy.setEnergyType(type);
        energy.setSpecial(false);
        energy.setProvidedEnergies(List.of(type));
        return energy;
    }

    public static EnergyCard createDoubleColorlessEnergy() {
        EnergyCard energy = new EnergyCard();
        energy.setId("dce-1");
        energy.setName("Double Colorless Energy");
        energy.setEnergyType(EnergyType.COLORLESS);
        energy.setSpecial(true);
        energy.setProvidedEnergies(List.of(EnergyType.COLORLESS, EnergyType.COLORLESS));
        return energy;
    }

    public static PlayerBoard createPlayerBoard(String playerId) {
        PlayerBoard board = new PlayerBoard();
        board.setPlayerId(playerId);
        return board;
    }

    public static PlayerBoard createPlayerBoardWithActive(String playerId, PokemonInPlay active) {
        PlayerBoard board = new PlayerBoard();
        board.setPlayerId(playerId);
        board.setActivePokemon(active);
        return board;
    }

    public static PlayerBoard createPlayerBoardWithBench(String playerId, List<PokemonInPlay> bench) {
        PlayerBoard board = new PlayerBoard();
        board.setPlayerId(playerId);
        for (PokemonInPlay p : bench) {
            if (p != null) board.getBench().add(p);
        }
        return board;
    }

    public static PlayerBoard createPlayerBoardWithActiveAndBench(String playerId, PokemonInPlay active, List<PokemonInPlay> bench) {
        PlayerBoard board = createPlayerBoardWithActive(playerId, active);
        for (PokemonInPlay p : bench) {
            if (p != null) board.getBench().add(p);
        }
        return board;
    }

    public static GameBoard createGameBoard(String player1Id, PokemonInPlay p1Active, String player2Id, PokemonInPlay p2Active) {
        GameBoard board = new GameBoard();
        board.setPlayer1Board(createPlayerBoardWithActive(player1Id, p1Active));
        board.setPlayer2Board(createPlayerBoardWithActive(player2Id, p2Active));
        return board;
    }

    public static AttackContext createAttackContext(PokemonInPlay attacker, PokemonInPlay defender, Attack attack, GameBoard board) {
        AttackContext ctx = new AttackContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setSelectedAttack(attack);
        ctx.setBaseDamage(attack != null ? attack.getDamage() : 0);
        ctx.setBoard(board);
        ctx.setAttackSuccessful(true);
        return ctx;
    }

    public static AttackContext createAttackContext() {
        return createAttackContext(createPokemonInPlay(), createPokemonInPlay(),
                createAttack("Tackle", List.of(EnergyType.COLORLESS), 30), null);
    }
}
