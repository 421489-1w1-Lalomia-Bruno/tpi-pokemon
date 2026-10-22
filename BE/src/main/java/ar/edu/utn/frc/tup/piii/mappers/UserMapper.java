package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponse;
import ar.edu.utn.frc.tup.piii.dtos.auth.UserDTO;
import ar.edu.utn.frc.tup.piii.dtos.auth.UserProfileDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserDTO toUserDTO(UserEntity entity) {
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId().toString());
        dto.setUsername(entity.getUsername());
        dto.setGender(entity.getGender());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setWins(entity.getWins());
        dto.setLosses(entity.getLosses());
        return dto;
    }

    public UserProfileDTO toProfileDTO(UserEntity entity, List<DeckDTO> decks) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(entity.getId().toString());
        dto.setUsername(entity.getUsername());
        dto.setGender(entity.getGender());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setWins(entity.getWins());
        dto.setLosses(entity.getLosses());
        dto.setDecks(decks);
        return dto;
    }

    public AuthResponse toAuthResponse(String token, UserEntity entity) {
        return new AuthResponse(token, toUserDTO(entity));
    }
}
