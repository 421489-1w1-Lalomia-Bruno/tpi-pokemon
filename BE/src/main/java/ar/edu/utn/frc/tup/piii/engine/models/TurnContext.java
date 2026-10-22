package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.TurnPhase;

import java.util.HashSet;
import java.util.Set;

public class TurnContext {

    private String currentPlayerId;
    private TurnPhase phase;
    private boolean isFirstTurn;
    private Set<String> completedFirstTurns = new HashSet<>();
    private boolean hasDrawn;
    private boolean hasPlayedSupporter;
    private boolean hasPlayedStadium;
    private boolean hasAttachedEnergy;
    private boolean hasEvolved;
    private boolean hasUsedAbility;
    private boolean hasRetreated;

    /** Epoch millis cuando comenzó el turno actual (fase ACTIONS). */
    private long turnStartTimeMs;

    public String getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(String currentPlayerId) { this.currentPlayerId = currentPlayerId; }

    public TurnPhase getPhase() { return phase; }
    public void setPhase(TurnPhase phase) { this.phase = phase; }

    public boolean isFirstTurn() { return isFirstTurn; }
    public void setFirstTurn(boolean firstTurn) { isFirstTurn = firstTurn; }

    public Set<String> getCompletedFirstTurns() { return completedFirstTurns; }
    public void setCompletedFirstTurns(Set<String> completedFirstTurns) { this.completedFirstTurns = completedFirstTurns; }

    public boolean isHasDrawn() { return hasDrawn; }
    public void setHasDrawn(boolean hasDrawn) { this.hasDrawn = hasDrawn; }

    public boolean isHasPlayedSupporter() { return hasPlayedSupporter; }
    public void setHasPlayedSupporter(boolean hasPlayedSupporter) { this.hasPlayedSupporter = hasPlayedSupporter; }

    public boolean isHasPlayedStadium() { return hasPlayedStadium; }
    public void setHasPlayedStadium(boolean hasPlayedStadium) { this.hasPlayedStadium = hasPlayedStadium; }

    public boolean isHasAttachedEnergy() { return hasAttachedEnergy; }
    public void setHasAttachedEnergy(boolean hasAttachedEnergy) { this.hasAttachedEnergy = hasAttachedEnergy; }

    public boolean isHasEvolved() { return hasEvolved; }
    public void setHasEvolved(boolean hasEvolved) { this.hasEvolved = hasEvolved; }

    public boolean isHasUsedAbility() { return hasUsedAbility; }
    public void setHasUsedAbility(boolean hasUsedAbility) { this.hasUsedAbility = hasUsedAbility; }

    public boolean isHasRetreated() { return hasRetreated; }
    public void setHasRetreated(boolean hasRetreated) { this.hasRetreated = hasRetreated; }

    public long getTurnStartTimeMs() { return turnStartTimeMs; }
    public void setTurnStartTimeMs(long turnStartTimeMs) { this.turnStartTimeMs = turnStartTimeMs; }
}
