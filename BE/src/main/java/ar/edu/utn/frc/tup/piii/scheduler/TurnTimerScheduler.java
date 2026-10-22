package ar.edu.utn.frc.tup.piii.scheduler;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.state.ActiveState;
import ar.edu.utn.frc.tup.piii.engine.state.MatchState;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TurnTimerScheduler {

    private static final Logger log = LoggerFactory.getLogger(TurnTimerScheduler.class);

    private final GameEngine gameEngine;

    public TurnTimerScheduler(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    @Scheduled(fixedRate = 1000)
    public void checkTimers() {
        try {
            Map<String, MatchState> states = gameEngine.getMatchStates();
            if (states == null) return;

            for (Map.Entry<String, MatchState> entry : states.entrySet()) {
                if (!(entry.getValue() instanceof ActiveState)) continue;

                String matchId = entry.getKey();
                MatchSnapshot snapshot = gameEngine.getSnapshot(matchId);
                if (snapshot == null || snapshot.getTurnContext() == null) continue;
                if (snapshot.getStatus() != MatchStatus.ACTIVE) continue;

                long now = System.currentTimeMillis();
                long elapsed = now - snapshot.getTurnContext().getTurnStartTimeMs();
                if (snapshot.getCurrentPlayerId() == null) continue;
                long remaining = snapshot.getCurrentPlayerId().equals(snapshot.getPlayer1Id())
                        ? snapshot.getPlayer1TimeRemainingMs()
                        : snapshot.getPlayer2TimeRemainingMs();
                long effective = remaining - elapsed;

                if (effective <= 0) {
                    String timedOutPlayer = snapshot.getCurrentPlayerId();
                    log.warn("Player {} timed out in match {}", timedOutPlayer, matchId);
                    gameEngine.declareLoser(matchId, timedOutPlayer, "Time out");
                }
            }
        } catch (Exception e) {
            log.error("Error in turn timer scheduler", e);
        }
    }
}
