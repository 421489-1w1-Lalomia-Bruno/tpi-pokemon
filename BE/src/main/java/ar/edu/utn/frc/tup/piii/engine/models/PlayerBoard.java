package ar.edu.utn.frc.tup.piii.engine.models;

import java.util.ArrayList;
import java.util.List;

public class PlayerBoard {

    private String playerId;
    private PokemonInPlay activePokemon;
    private List<PokemonInPlay> bench;
    private List<GameCard> hand;
    private List<GameCard> deck;
    private List<GameCard> discardPile;
    private List<GameCard> prizeCards;
    private Integer prizeCardsTaken;
    private int pendingPrizeCount;
    private List<GameCard> revealedCards;
    private boolean hasPendingReveal;

    public PlayerBoard() {
        this.bench = new ArrayList<>();
        this.hand = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.prizeCards = new ArrayList<>();
        this.prizeCardsTaken = 0;
        this.pendingPrizeCount = 0;
        this.revealedCards = null;
        this.hasPendingReveal = false;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public PokemonInPlay getActivePokemon() { return activePokemon; }
    public void setActivePokemon(PokemonInPlay activePokemon) {
        if (this.activePokemon != null) {
            this.activePokemon.setActive(false);
        }
        this.activePokemon = activePokemon;
        if (activePokemon != null) {
            activePokemon.setActive(true);
        }
    }

    public List<PokemonInPlay> getBench() { return bench; }
    public void setBench(List<PokemonInPlay> bench) { this.bench = bench; }

    public List<GameCard> getHand() { return hand; }
    public void setHand(List<GameCard> hand) { this.hand = hand; }

    public List<GameCard> getDeck() { return deck; }
    public void setDeck(List<GameCard> deck) { this.deck = deck; }

    public List<GameCard> getDiscardPile() { return discardPile; }
    public void setDiscardPile(List<GameCard> discardPile) { this.discardPile = discardPile; }

    public List<GameCard> getPrizeCards() { return prizeCards; }
    public void setPrizeCards(List<GameCard> prizeCards) { this.prizeCards = prizeCards; }

    public Integer getPrizeCardsTaken() { return prizeCardsTaken; }
    public void setPrizeCardsTaken(Integer prizeCardsTaken) { this.prizeCardsTaken = prizeCardsTaken; }

    public int getPendingPrizeCount() { return pendingPrizeCount; }
    public void setPendingPrizeCount(int pendingPrizeCount) { this.pendingPrizeCount = pendingPrizeCount; }

    public List<GameCard> getRevealedCards() { return revealedCards; }
    public void setRevealedCards(List<GameCard> revealedCards) { this.revealedCards = revealedCards; }

    public boolean isHasPendingReveal() { return hasPendingReveal; }
    public void setHasPendingReveal(boolean hasPendingReveal) { this.hasPendingReveal = hasPendingReveal; }

    private int pendingMultiCoinTargets;
    private String pendingMultiCoinEnergyType;

    public int getPendingMultiCoinTargets() { return pendingMultiCoinTargets; }
    public void setPendingMultiCoinTargets(int pendingMultiCoinTargets) { this.pendingMultiCoinTargets = pendingMultiCoinTargets; }

    public String getPendingMultiCoinEnergyType() { return pendingMultiCoinEnergyType; }
    public void setPendingMultiCoinEnergyType(String pendingMultiCoinEnergyType) { this.pendingMultiCoinEnergyType = pendingMultiCoinEnergyType; }
}
