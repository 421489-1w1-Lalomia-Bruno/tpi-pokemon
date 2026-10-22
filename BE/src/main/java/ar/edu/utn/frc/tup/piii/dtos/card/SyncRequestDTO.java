package ar.edu.utn.frc.tup.piii.dtos.card;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SyncRequestDTO {
    @NotBlank
    private String setId = "xy1";
}
