package ar.edu.utn.frc.tup.piii.websocket.messages;

import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameActionMessage {
    private String matchId;
    // playerId is NOT included. Extracted from Principal in the_authenticated session.
    private GameActionType type;
    private Map<String, Object> payload;
    private String timestamp;
}
