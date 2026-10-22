package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GameEventListener {

    private static final Logger log = LoggerFactory.getLogger(GameEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final GameEngine gameEngine;

    public GameEventListener(SimpMessagingTemplate messagingTemplate, GameEngine gameEngine) {
        this.messagingTemplate = messagingTemplate;
        this.gameEngine = gameEngine;
    }

    @EventListener
    public void handleGameEvent(GameEvent event) {
        String eventType = event.getType() != null ? event.getType().name() : null;
        String matchId = event.getMatchId();

        log.debug("Game event received: {} for match {}", eventType, matchId);

        String messageType;
        String notification = null;

        if (eventType == null) {
            messageType = "NOTIFICATION";
            notification = "Game event";
        } else {
            switch (eventType) {
                case "ATTACK_FAILED" -> {
                    messageType = "NOTIFICATION";
                    notification = event.getDescription();
                }
                case "TURN_CHANGED" -> {
                    messageType = "NOTIFICATION";
                    notification = event.getDescription() != null ? event.getDescription() : "Turn changed";
                }
                case "PHASE_CHANGED" -> {
                    messageType = "NOTIFICATION";
                    notification = event.getDescription() != null ? event.getDescription() : "Phase changed";
                }
                case "CARD_DRAWN", "POKEMON_PLACED", "ENERGY_ATTACHED", "TRAINER_PLAYED",
                     "POKEMON_EVOLVED", "POKEMON_RETREATED", "ATTACK_EXECUTED",
                     "BETWEEN_TURNS_PROCESSED", "GAME_FINISHED",
                     "POKEMON_KNOCKED_OUT", "PRIZE_TAKEN", "SPECIAL_CONDITION_APPLIED",
                     "SPECIAL_CONDITION_REMOVED", "COIN_FLIP_RESULT",
                     "REVEAL_CARDS", "MULTI_COIN_FLIP" -> messageType = "EVENT";
                case "NOTIFICATION" -> {
                    messageType = "NOTIFICATION";
                    notification = event.getDescription();
                }
                case "ABILITY_USED" -> {
                    messageType = "NOTIFICATION";
                    notification = event.getDescription();
                }
                default -> {
                    messageType = "NOTIFICATION";
                    notification = "Game event: " + eventType;
                }
            }
        }

        MatchSnapshot snapshot = gameEngine.getSnapshot(matchId);
        if (snapshot == null) {
            log.warn("Cannot deliver event {} for match {}: no snapshot available", eventType, matchId);
            return;
        }

        sendEventToPlayer(snapshot.getPlayer1Id(), event, messageType, notification);
        if (snapshot.getPlayer2Id() != null) {
            sendEventToPlayer(snapshot.getPlayer2Id(), event, messageType, notification);
        }
    }

    private void sendEventToPlayer(String targetPlayerId, GameEvent event, String messageType, String notification) {
        Map<String, Object> message = new HashMap<>();
        message.put("event", event);
        message.put("type", messageType);
        if (notification != null) {
            message.put("notification", notification);
        }
        messagingTemplate.convertAndSendToUser(targetPlayerId, "/queue/match", message);
    }

}
