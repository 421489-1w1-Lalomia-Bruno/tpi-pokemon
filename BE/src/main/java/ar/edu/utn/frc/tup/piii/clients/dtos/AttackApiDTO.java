package ar.edu.utn.frc.tup.piii.clients.dtos;

import lombok.Data;
import java.util.List;

@Data
public class AttackApiDTO {
    private String name;
    private List<String> cost;
    private String convertedEnergyCost;
    private String damage;
    private String text;
}
