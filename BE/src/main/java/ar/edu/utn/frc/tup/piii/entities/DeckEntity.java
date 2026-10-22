package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.enums.DeckSource;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decks")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeckEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "creator_id", length = 100)
    private String creatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false)
    @Builder.Default
    private DeckSource source = DeckSource.USER;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeckCardEntity> cards = new ArrayList<>();
}
