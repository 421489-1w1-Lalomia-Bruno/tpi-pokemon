package ar.edu.utn.frc.tup.piii.dtos.card;

import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import lombok.Data;

import java.util.List;

@Data
public class AttackDTO {
    private String name;
    private List<EnergyType> cost;
    private int damage;
    private String effectText;
}
