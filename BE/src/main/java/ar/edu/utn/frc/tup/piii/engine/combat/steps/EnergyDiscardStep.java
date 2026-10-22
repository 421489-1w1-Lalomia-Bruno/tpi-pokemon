package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.engine.models.*;

import java.util.ArrayList;
import java.util.List;

public class EnergyDiscardStep implements AttackStep {

    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        Attack attack = context.getSelectedAttack();
        if (attack == null || attack.getCost() == null || attack.getCost().isEmpty()) {
            executeNext(context);
            return;
        }

        PokemonInPlay attacker = context.getAttacker();
        List<EnergyCard> attachedEnergy = attacker.getAttachedEnergy();
        if (attachedEnergy == null || attachedEnergy.isEmpty()) {
            executeNext(context);
            return;
        }

        GameBoard board = context.getBoard();
        String currentPlayerId = context.getCurrentPlayerId();
        PlayerBoard playerBoard = findPlayerBoard(board, currentPlayerId);
        if (playerBoard == null) {
            executeNext(context);
            return;
        }

        List<EnergyType> cost = attack.getCost();
        List<EnergyType> specific = new ArrayList<>();
        int colorless = 0;
        for (EnergyType req : cost) {
            if (req == EnergyType.COLORLESS) colorless++;
            else specific.add(req);
        }

        List<EnergyCard> pool = new ArrayList<>(attachedEnergy);
        List<EnergyCard> toDiscard = new ArrayList<>();

        for (EnergyType req : specific) {
            EnergyCard found = findCardProviding(pool, req);
            if (found == null) {
                found = findWildcard(pool);
            }
            if (found != null) {
                pool.remove(found);
                toDiscard.add(found);
            }
        }

        int assigned = 0;
        while (assigned < colorless && !pool.isEmpty()) {
            EnergyCard card = pool.remove(0);
            toDiscard.add(card);
            int cardColorless = card.getProvidedEnergies() != null
                    ? (int) card.getProvidedEnergies().stream().filter(e -> e == EnergyType.COLORLESS).count()
                    : 0;
            assigned += Math.max(1, cardColorless);
        }

        for (EnergyCard energy : toDiscard) {
            attachedEnergy.remove(energy);
            playerBoard.getDiscardPile().add(energy);
        }

        executeNext(context);
    }

    private EnergyCard findCardProviding(List<EnergyCard> energies, EnergyType type) {
        for (EnergyCard card : energies) {
            if (card.getProvidedEnergies() != null && card.getProvidedEnergies().contains(type)) {
                return card;
            }
        }
        return null;
    }

    private EnergyCard findWildcard(List<EnergyCard> energies) {
        for (EnergyCard card : energies) {
            if (card.isWildcard()) {
                return card;
            }
        }
        return null;
    }

    private PlayerBoard findPlayerBoard(GameBoard board, String playerId) {
        if (board.getPlayer1Board() != null && playerId.equals(board.getPlayer1Board().getPlayerId())) {
            return board.getPlayer1Board();
        }
        if (board.getPlayer2Board() != null && playerId.equals(board.getPlayer2Board().getPlayerId())) {
            return board.getPlayer2Board();
        }
        return null;
    }

    @Override
    public void setNext(AttackStep next) {
        this.next = next;
    }

    private void executeNext(AttackContext context) {
        if (next != null && context.isAttackSuccessful()) {
            next.execute(context);
        }
    }
}
