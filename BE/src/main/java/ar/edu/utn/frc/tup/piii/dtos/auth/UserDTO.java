package ar.edu.utn.frc.tup.piii.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import ar.edu.utn.frc.tup.piii.enums.UserGender;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String id;
    private String username;
    private UserGender gender;
    private LocalDateTime createdAt;
    private int wins;
    private int losses;
}
