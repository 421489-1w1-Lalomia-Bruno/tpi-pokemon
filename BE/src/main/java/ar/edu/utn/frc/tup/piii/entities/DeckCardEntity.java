package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deck_cards")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeckCardEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private DeckEntity deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private CardEntity card;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
