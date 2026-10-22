package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.CardType;

public class HiddenCard implements GameCard {

    private String id;

    public HiddenCard() {
        // Jackson deserialization
    }

    public HiddenCard(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return "???";
    }

    @Override
    public CardType getType() {
        return CardType.HIDDEN;
    }
}
