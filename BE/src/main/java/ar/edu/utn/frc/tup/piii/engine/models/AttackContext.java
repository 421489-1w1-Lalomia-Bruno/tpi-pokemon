package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.EffectCondition;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

import java.util.ArrayList;
import java.util.List;

public class AttackContext {

    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private Attack selectedAttack;
    private Integer baseDamage;
    private Integer finalDamage;
    private List<SpecialCondition> effectsToApply;
    private boolean attackSuccessful;
    private String failureReason;
    private GameBoard board;
    private String currentPlayerId;
    private String matchId;
    private GameEventPublisher eventPublisher;
    private GameCard revealedCard;
    private List<GameCard> faceDownCardsForReveal;
    private List<GameCard> opponentHandForReveal;
    private String opponentPlayerIdForReveal;
    private String targetId;
    private String variableDamageZeroDescription;
    private boolean flipResolvedByAttack;
    private EffectCondition lastFlipResult;
    private Boolean conditionalFlipHeads;
    private boolean needsConditionalFlip;
    private String branchEffectText;

    public AttackContext() {
        this.effectsToApply = new ArrayList<>();
        this.attackSuccessful = true;
    }

    public PokemonInPlay getAttacker() { return attacker; }
    public void setAttacker(PokemonInPlay attacker) { this.attacker = attacker; }

    public PokemonInPlay getDefender() { return defender; }
    public void setDefender(PokemonInPlay defender) { this.defender = defender; }

    public Attack getSelectedAttack() { return selectedAttack; }
    public void setSelectedAttack(Attack selectedAttack) { this.selectedAttack = selectedAttack; }

    public Integer getBaseDamage() { return baseDamage; }
    public void setBaseDamage(Integer baseDamage) { this.baseDamage = baseDamage; }

    public Integer getFinalDamage() { return finalDamage; }
    public void setFinalDamage(Integer finalDamage) { this.finalDamage = finalDamage; }

    public List<SpecialCondition> getEffectsToApply() { return effectsToApply; }
    public void setEffectsToApply(List<SpecialCondition> effectsToApply) { this.effectsToApply = effectsToApply; }

    public boolean isAttackSuccessful() { return attackSuccessful; }
    public void setAttackSuccessful(boolean attackSuccessful) { this.attackSuccessful = attackSuccessful; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public GameBoard getBoard() { return board; }
    public void setBoard(GameBoard board) { this.board = board; }

    public String getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(String currentPlayerId) { this.currentPlayerId = currentPlayerId; }

    public GameCard getRevealedCard() { return revealedCard; }
    public void setRevealedCard(GameCard revealedCard) { this.revealedCard = revealedCard; }

    public List<GameCard> getFaceDownCardsForReveal() { return faceDownCardsForReveal; }
    public void setFaceDownCardsForReveal(List<GameCard> faceDownCardsForReveal) { this.faceDownCardsForReveal = faceDownCardsForReveal; }

    public List<GameCard> getOpponentHandForReveal() { return opponentHandForReveal; }
    public void setOpponentHandForReveal(List<GameCard> opponentHandForReveal) { this.opponentHandForReveal = opponentHandForReveal; }

    public String getOpponentPlayerIdForReveal() { return opponentPlayerIdForReveal; }
    public void setOpponentPlayerIdForReveal(String opponentPlayerIdForReveal) { this.opponentPlayerIdForReveal = opponentPlayerIdForReveal; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public GameEventPublisher getEventPublisher() { return eventPublisher; }
    public void setEventPublisher(GameEventPublisher eventPublisher) { this.eventPublisher = eventPublisher; }

    public String getVariableDamageZeroDescription() { return variableDamageZeroDescription; }
    public void setVariableDamageZeroDescription(String variableDamageZeroDescription) { this.variableDamageZeroDescription = variableDamageZeroDescription; }

    public boolean isFlipResolvedByAttack() { return flipResolvedByAttack; }
    public void setFlipResolvedByAttack(boolean flipResolvedByAttack) { this.flipResolvedByAttack = flipResolvedByAttack; }

    public EffectCondition getLastFlipResult() { return lastFlipResult; }
    public void setLastFlipResult(EffectCondition lastFlipResult) { this.lastFlipResult = lastFlipResult; }

    public Boolean isConditionalFlipHeads() { return conditionalFlipHeads; }
    public void setConditionalFlipHeads(Boolean conditionalFlipHeads) { this.conditionalFlipHeads = conditionalFlipHeads; }

    public boolean isNeedsConditionalFlip() { return needsConditionalFlip; }
    public void setNeedsConditionalFlip(boolean needsConditionalFlip) { this.needsConditionalFlip = needsConditionalFlip; }

    public String getBranchEffectText() { return branchEffectText; }
    public void setBranchEffectText(String branchEffectText) { this.branchEffectText = branchEffectText; }

    private int pendingMultiCoinTargets;
    private String pendingMultiCoinEnergyType;

    public int getPendingMultiCoinTargets() { return pendingMultiCoinTargets; }
    public void setPendingMultiCoinTargets(int pendingMultiCoinTargets) { this.pendingMultiCoinTargets = pendingMultiCoinTargets; }

    public String getPendingMultiCoinEnergyType() { return pendingMultiCoinEnergyType; }
    public void setPendingMultiCoinEnergyType(String pendingMultiCoinEnergyType) { this.pendingMultiCoinEnergyType = pendingMultiCoinEnergyType; }

    public boolean isPausedForTargetSelection() { return pendingMultiCoinTargets > 0; }
}
