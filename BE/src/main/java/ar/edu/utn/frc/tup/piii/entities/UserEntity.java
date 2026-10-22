package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ar.edu.utn.frc.tup.piii.enums.UserGender;
import jakarta.persistence.Enumerated;
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity extends BaseEntity{
     @Column(nullable = false, unique = true, length = 20)
    private String username;
     @Column(name = "password_hash", nullable = false,length = 60)
    private String passwordHash;
     @Enumerated(EnumType.STRING)
      @Column(name = "gender")
    private UserGender gender;

    @Column(nullable = false)
    @Builder.Default
    private int wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private int losses = 0;
}
