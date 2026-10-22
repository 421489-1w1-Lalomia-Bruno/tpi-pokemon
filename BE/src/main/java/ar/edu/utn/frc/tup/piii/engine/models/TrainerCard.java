package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.CardType;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;

public class TrainerCard implements  GameCard {
    private String id;
    private String name;
    private TrainerSubtype subtype;
    private String effect;
    private String imageUrl;

    @Override
    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }

    @Override
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public CardType getType() { return CardType.TRAINER; }

    public TrainerSubtype getSubtype() { return subtype; }
    public void setSubtype(TrainerSubtype subtype) { this.subtype = subtype; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
