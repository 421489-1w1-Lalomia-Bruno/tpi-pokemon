package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "cards")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardEntity extends BaseEntity {

    @Column(name = "external_id", unique = true, nullable = false, length = 50)
    private String externalId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "supertype", nullable = false, length = 30)
    private String supertype;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subtypes")
    private List<String> subtypes;

    @Column(name = "hp")
    private Integer hp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "types")
    private Set<EnergyType> types;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 20)
    private PokemonStage stage;

    @Column(name = "evolves_from", length = 100)
    private String evolvesFrom;

    @Column(name = "retreat_cost")
    private Integer retreatCost;

    @Column(name = "weakness_type", length = 30)
    private String weaknessType;

    @Column(name = "resistance_type", length = 30)
    private String resistanceType;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "attacks_json", columnDefinition = "TEXT")
    private String attacksJson;

    @Column(name = "abilities_json", columnDefinition = "TEXT")
    private String abilitiesJson;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "rarity", length = 50)
    private String rarity;

    @Column(name = "card_number", nullable = false, length = 20)
    private String cardNumber;

    @Column(name = "set_id", length = 20)
    @Builder.Default
    private String setId = "xy1";
}
