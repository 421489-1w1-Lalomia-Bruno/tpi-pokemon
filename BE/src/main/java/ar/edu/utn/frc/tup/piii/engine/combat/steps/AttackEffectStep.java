package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectContext;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.PreventDamageEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.AttackEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.EffectCondition;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.EffectOperation;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.EffectTiming;
import ar.edu.utn.frc.tup.piii.engine.effects.attack.TargetScope;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;

import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("PMD.SwitchDensity")
public class AttackEffectStep implements AttackStep {

    private final StatusEffectManager statusEffectManager;
    private final AbilityEffectResolver abilityResolver;
    private final AttackEffectResolver attackEffectResolver;
    private AttackStep next;

    public AttackEffectStep() {
        this.statusEffectManager = new StatusEffectManager();
        this.abilityResolver = new AbilityEffectResolver();
        this.attackEffectResolver = new AttackEffectResolver();
    }

    public AttackEffectStep(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = statusEffectManager;
        this.abilityResolver = new AbilityEffectResolver();
        this.attackEffectResolver = new AttackEffectResolver();
    }

    @Override
    public void execute(AttackContext context) {
        if (!context.isAttackSuccessful()) return;

        String effectText = context.getSelectedAttack() != null
                ? context.getSelectedAttack().getEffectText() : null;

        List<AttackEffect> effects = attackEffectResolver.resolve(effectText);

        boolean hasInsteadDamage = effects.stream()
                .anyMatch(e -> e.getTiming() == EffectTiming.INSTEAD_OF_DAMAGE);

        if (hasInsteadDamage) {
            executeEffects(effects, context);
            executeNext(context);
            return;
        }

        PokemonInPlay defender = context.getDefender();
        Integer finalDamage = context.getFinalDamage();

        int prevented = 0;
        if (finalDamage != null && finalDamage > 0) {
            prevented = applyDefenderPrevention(defender, finalDamage, context);
        }
        int actualDamage = Math.max((finalDamage != null ? finalDamage : 0) - prevented, 0);
        if (actualDamage > 0 && defender != null) {
            defender.takeDamage(actualDamage);
        }

        List<SpecialCondition> conditions = context.getEffectsToApply();
        if (conditions != null && !conditions.isEmpty() && defender != null) {
            // If a conditional flip was already done in ActiveState, all conditions in effectsToApply
            // were determined by that flip — apply them all directly and skip FLIP_COIN_STATUS re-flip.
            boolean alreadyFlipped = context.isConditionalFlipHeads() != null;
            if (alreadyFlipped) {
                for (SpecialCondition effect : conditions) {
                    statusEffectManager.addEffect(defender, effect);
                }
            } else {
                List<String> flipStatusTypes = effects.stream()
                        .filter(e -> e.getOperation() == EffectOperation.FLIP_COIN_STATUS)
                        .map(AttackEffect::getSubType)
                        .filter(s -> s != null)
                        .map(String::toUpperCase)
                        .toList();
                for (SpecialCondition effect : conditions) {
                    if (flipStatusTypes.contains(effect.name())) continue;
                    statusEffectManager.addEffect(defender, effect);
                }
            }
        }

        // If attack targets "both" active Pokémon (e.g. "Confusion Wave"), also apply to attacker
        PokemonInPlay attacker = context.getAttacker();
        String atkText = context.getSelectedAttack() != null
                ? context.getSelectedAttack().getEffectText() : null;
        if (attacker != null && atkText != null && conditions != null && !conditions.isEmpty()) {
            String lower = atkText.toLowerCase();
            if (lower.contains("both") || lower.contains("ambos")) {
                for (SpecialCondition sc : conditions) {
                    statusEffectManager.addEffect(attacker, sc);
                }
            }
        }

        executeEffects(effects, context);

        executeNext(context);
    }

    private void executeEffects(List<AttackEffect> effects, AttackContext ctx) {
        if (effects.isEmpty()) return;

        GameBoard board = ctx.getBoard();
        String playerId = ctx.getCurrentPlayerId();
        PokemonInPlay attacker = ctx.getAttacker();
        PokemonInPlay defender = ctx.getDefender();

        PlayerBoard sourceBoard = null;
        PlayerBoard oppBoard = null;
        if (board != null) {
            if (board.getPlayer1Board() != null && playerId.equals(board.getPlayer1Board().getPlayerId())) {
                sourceBoard = board.getPlayer1Board();
                oppBoard = board.getPlayer2Board();
            } else if (board.getPlayer2Board() != null) {
                sourceBoard = board.getPlayer2Board();
                oppBoard = board.getPlayer1Board();
            }
        }
        if (sourceBoard == null) return;

        String sourceName = attacker != null ? attacker.getCard().getName() : "unknown";

        AttackEffectContext actx = new AttackEffectContext(
                ctx.getMatchId(), playerId, attacker, defender,
                board, sourceBoard, oppBoard,
                ctx.getEventPublisher(), statusEffectManager, sourceName, ctx);

        for (AttackEffect effect : effects) {
            if (effect.getTiming() == EffectTiming.INSTEAD_OF_DAMAGE && ctx.getFinalDamage() != null && ctx.getFinalDamage() > 0) {
                continue;
            }
            if (effect.getMinDamage() > 0 && (ctx.getFinalDamage() == null || ctx.getFinalDamage() < effect.getMinDamage())) {
                continue;
            }
            executeOne(effect, actx);
        }
    }

    private void executeOne(AttackEffect effect, AttackEffectContext ctx) {
        switch (effect.getOperation()) {
            case DRAW:
                GameActionUtils.drawCards(ctx.getSourceBoard(), effect.getAmount(),
                        ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                break;

            case DRAW_THEN_SHUFFLE:
                GameActionUtils.drawCards(ctx.getSourceBoard(), effect.getAmount(),
                        false, true,
                        ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                break;

            case DRAW_OPPONENT_SHUFFLE:
                if (ctx.getOpponentBoard() != null) {
                    GameActionUtils.redCardEffect(ctx.getOpponentBoard(), effect.getAmount(),
                            ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                }
                break;

            case HEAL:
                if (effect.getTarget() == TargetScope.SELF_BENCH && ctx.getSourceBoard() != null) {
                    for (PokemonInPlay bench : ctx.getSourceBoard().getBench()) {
                        if (bench != null) GameActionUtils.healDamage(bench, effect.getAmount());
                    }
                } else if (ctx.getAttacker() != null) {
                    GameActionUtils.healDamage(ctx.getAttacker(), effect.getAmount());
                }
                break;

            case SWITCH_SELF:
                GameActionUtils.switchActive(ctx.getSourceBoard(), null,
                        ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                break;

            case SWITCH_OPPONENT:
                if (ctx.getOpponentBoard() != null) {
                    String targetId = ctx.getAttackContext().getTargetId();
                    GameActionUtils.switchActive(ctx.getOpponentBoard(), targetId,
                            ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                }
                break;

            case DISCARD_ENERGY: {
                EffectCondition deCond = effect.getCondition();
                boolean deExecute = true;
                if (deCond != EffectCondition.ALWAYS) {
                    Boolean storedFlip = ctx.getAttackContext().isConditionalFlipHeads();
                    if (storedFlip != null) {
                        EffectCondition flipResult = storedFlip ? EffectCondition.HEADS : EffectCondition.TAILS;
                        deExecute = (flipResult == deCond);
                    }
                }
                if (deExecute && ctx.getDefender() != null && ctx.getOpponentBoard() != null) {
                    GameActionUtils.discardEnergy(ctx.getDefender(), false, ctx.getOpponentBoard(),
                            ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                }
                break;
            }

            case DISCARD_ENERGY_SELF: {
                EffectCondition desCond = effect.getCondition();
                boolean desExecute = true;
                if (desCond != EffectCondition.ALWAYS) {
                Boolean storedFlip2 = ctx.getAttackContext().isConditionalFlipHeads();
                        if (storedFlip2 != null) {
                            EffectCondition flipResult = storedFlip2 ? EffectCondition.HEADS : EffectCondition.TAILS;
                            desExecute = (flipResult == desCond);
                        }
                }
                if (desExecute && ctx.getAttacker() != null && ctx.getSourceBoard() != null) {
                    GameActionUtils.discardEnergy(ctx.getAttacker(), false, ctx.getSourceBoard(),
                            ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                }
                break;
            }

            case DISCARD_ALL_ENERGY_SELF: {
                PokemonInPlay allAttacker = ctx.getAttacker();
                if (allAttacker != null && ctx.getSourceBoard() != null) {
                    List<EnergyCard> allEnergy = new ArrayList<>(allAttacker.getAttachedEnergy());
                    allAttacker.getAttachedEnergy().clear();
                    ctx.getSourceBoard().getDiscardPile().addAll(allEnergy);
                }
                break;
            }

            case FLIP_COIN_STATUS:
                if (ctx.getAttackContext().isConditionalFlipHeads() != null) break;
                if (ctx.getDefender() != null && effect.getSubType() != null) {
                    SpecialCondition sc = parseCondition(effect.getSubType());
                    if (sc != null) {
                        GameActionUtils.flipCoinApplyStatus(ctx.getPublisher(), ctx.getMatchId(),
                                ctx.getPlayerId(), ctx.getSourceName(), ctx.getDefender(), sc, statusEffectManager);
                    }
                }
                break;

            case DAMAGE_BENCH:
                if (ctx.getOpponentBoard() != null) {
                    int count = 0;
                    int max = effect.getTargetCount();
                    for (PokemonInPlay bench : ctx.getOpponentBoard().getBench()) {
                        if (bench != null) {
                            bench.takeDamage(effect.getAmount());
                            count++;
                            if (max > 0 && count >= max) break;
                        }
                    }
                }
                break;

            case DAMAGE_BENCH_SPLASH:
                if (ctx.getOpponentBoard() != null) {
                    int applied = 0;
                    int max = effect.getTargetCount();
                    if (max <= 0) max = ctx.getOpponentBoard().getBench().size();
                    for (PokemonInPlay bench : ctx.getOpponentBoard().getBench()) {
                        if (bench != null && applied < max) {
                            bench.takeDamage(effect.getAmount());
                            applied++;
                        }
                    }
                }
                break;

            case SELF_BENCH_DAMAGE:
                if (ctx.getSourceBoard() != null) {
                    for (PokemonInPlay bench : ctx.getSourceBoard().getBench()) {
                        if (bench != null) {
                            bench.takeDamage(effect.getAmount());
                        }
                    }
                }
                break;

            case DAMAGE_SELF:
                if (ctx.getAttacker() != null) {
                    ctx.getAttacker().takeDamage(effect.getAmount());
                }
                break;

            case ENERGY_ATTACH:
                if (ctx.getAttacker() != null) {
                    List<GameCard> deck = ctx.getSourceBoard().getDeck();
                    String requiredType = effect.getSubType();
                    for (GameCard card : deck) {
                        if (card instanceof EnergyCard energy) {
                            if (requiredType == null || energy.getType().name().equalsIgnoreCase(requiredType)) {
                                deck.remove(energy);
                                ctx.getAttacker().getAttachedEnergy().add(energy);
                                break;
                            }
                        }
                    }
                }
                break;

            case MULTI_COIN: {
                if (ctx.getSourceBoard() != null) {
                    int maxCoins = effect.getAmount() > 0 ? effect.getAmount() : 3;
                    int heads = 0;
                    for (int i = 0; i < maxCoins; i++) {
                        if (CoinFlipManager.flip()) heads++;
                    }
                    if (heads == 0) {
                        if (ctx.getPublisher() != null) {
                            ctx.getPublisher().publish(GameEventType.NOTIFICATION,
                                    ctx.getMatchId(), ctx.getPlayerId(),
                                    ctx.getSourceName() + ": No salió cara", ctx.getSourceName(), null, 0);
                        }
                        break;
                    }

                    String encoded = effect.getSubType();
                    if (encoded != null && encoded.startsWith("energy_attach:")) {
                        String rawType = encoded.substring("energy_attach:".length());
                        final String requiredType = (rawType != null && rawType.isEmpty()) ? null : rawType;

                        List<GameCard> discard = ctx.getSourceBoard().getDiscardPile();
                        long available = discard.stream()
                                .filter(c -> c instanceof EnergyCard e
                                        && (requiredType == null || e.getType().name().equalsIgnoreCase(requiredType)))
                                .count();
                        int actual = Math.min(heads, (int) available);

                        if (actual == 0) {
                            if (ctx.getPublisher() != null) {
                                ctx.getPublisher().publish(GameEventType.NOTIFICATION,
                                        ctx.getMatchId(), ctx.getPlayerId(),
                                        ctx.getSourceName() + ": No hay energía disponible", ctx.getSourceName(), null, 0);
                            }
                            break;
                        }

                        boolean interactive = ctx.getPublisher() != null
                                && effect.getTarget() == TargetScope.SELF_BENCH;
                        if (interactive) {
                            AttackContext ac = ctx.getAttackContext();
                            ac.setPendingMultiCoinTargets(actual);
                            ac.setPendingMultiCoinEnergyType(requiredType);
                            if (ctx.getPublisher() != null) {
                                ctx.getPublisher().publish(GameEventType.NOTIFICATION,
                                        ctx.getMatchId(), ctx.getPlayerId(),
                                        ctx.getSourceName() + ": Selecciona un Pokémon de la banca para adjuntar energía",
                                        ctx.getSourceName(), null, actual);
                            }
                            break;
                        }

                        for (int a = 0; a < actual; a++) {
                            Iterator<GameCard> it = discard.iterator();
                            while (it.hasNext()) {
                                GameCard card = it.next();
                                if (card instanceof EnergyCard energy) {
                                    if (requiredType == null || energy.getType().name().equalsIgnoreCase(requiredType)) {
                                        it.remove();
                                        PokemonInPlay target;
                                        if (effect.getTarget() == TargetScope.SELF_BENCH
                                                && !ctx.getSourceBoard().getBench().isEmpty()) {
                                            target = ctx.getSourceBoard().getBench().get(0);
                                        } else {
                                            target = ctx.getAttacker();
                                        }
                                        if (target != null) {
                                            target.getAttachedEnergy().add(energy);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }

            case ENERGY_ATTACH_FROM_DISCARD: {
                if (ctx.getSourceBoard() != null) {
                    List<GameCard> discard = ctx.getSourceBoard().getDiscardPile();
                    String requiredType = effect.getSubType();
                    Iterator<GameCard> it = discard.iterator();
                    while (it.hasNext()) {
                        GameCard card = it.next();
                        if (card instanceof EnergyCard energy) {
                            if (requiredType == null || energy.getType().name().equalsIgnoreCase(requiredType)) {
                                it.remove();
                                PokemonInPlay target;
                                if (effect.getTarget() == TargetScope.SELF_BENCH && !ctx.getSourceBoard().getBench().isEmpty()) {
                                    target = ctx.getSourceBoard().getBench().get(0);
                                } else {
                                    target = ctx.getAttacker();
                                }
                                if (target != null) {
                                    target.getAttachedEnergy().add(energy);
                                }
                                break;
                            }
                        }
                    }
                }
                break;
            }

            case RECYCLE: {
                String subType = effect.getSubType();
                boolean toDeckTop = "deck_top".equals(subType);
                String effectiveType = toDeckTop ? null : (subType != null ? subType : "pokemon");
                int amount = effect.getAmount() > 0 ? effect.getAmount() : 1;
                String destination = toDeckTop ? "deck" : "hand";
                GameActionUtils.retrieveFromDiscard(ctx.getSourceBoard(), effectiveType, false, amount, destination, toDeckTop);
                break;
            }

            case DISCARD_OPPONENT:
                if (ctx.getOpponentBoard() != null) {
                    GameActionUtils.discardCards(ctx.getOpponentBoard(), effect.getAmount(),
                            ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                }
                break;

            case DISCARD:
                GameActionUtils.discardCards(ctx.getSourceBoard(), effect.getAmount(),
                        ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                break;

            case SHUFFLE_HAND_INTO_DECK:
                GameActionUtils.shuffleHandIntoDeck(ctx.getSourceBoard(),
                        ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                break;

            case SHUFFLE_DECK:
                GameActionUtils.shuffleDeck(ctx.getSourceBoard(),
                        ctx.getPublisher(), ctx.getMatchId(), ctx.getPlayerId(), ctx.getSourceName());
                break;

            case SEARCH:
                GameActionUtils.searchDeck(ctx.getSourceBoard(), effect.getSubType(), false, effect.getAmount() > 0 ? effect.getAmount() : 1);
                break;

            case ADD_DAMAGE:
                if (ctx.getDefender() != null) {
                    EffectCondition cond = effect.getCondition();
                    EffectCondition result = EffectCondition.ALWAYS;

                    if (cond != EffectCondition.ALWAYS) {
                Boolean storedFlip = ctx.getAttackContext().isConditionalFlipHeads();
                        if (storedFlip != null) {
                            // Reuse the flip already done in ActiveState — no second flip
                            result = storedFlip ? EffectCondition.HEADS : EffectCondition.TAILS;
                            ctx.getAttackContext().setLastFlipResult(result);
                        } else {
                            boolean heads = ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager.flip();
                            result = heads ? EffectCondition.HEADS : EffectCondition.TAILS;
                            ctx.getAttackContext().setLastFlipResult(result);
                            if (ctx.getPublisher() != null) {
                                ctx.getPublisher().publish(GameEventType.COIN_FLIP_RESULT,
                                        ctx.getMatchId(), ctx.getPlayerId(),
                                        ctx.getSourceName() + ": " + (heads ? "Heads" : "Tails"),
                                        ctx.getSourceName(), null, heads ? 1 : 0);
                            }
                        }
                    }

                    if (result == cond || cond == EffectCondition.ALWAYS) {
                        ctx.getDefender().takeDamage(effect.getAmount());
                    }
                }
                break;

            case DISCARD_FROM_DECK: {
                PlayerBoard targetBoard = effect.getTarget() == TargetScope.OPPONENT
                        ? ctx.getOpponentBoard() : ctx.getSourceBoard();
                if (targetBoard == null || targetBoard.getDeck().isEmpty()) break;

                int count = effect.getAmount() > 0 ? effect.getAmount() : 1;
                List<GameCard> discarded = GameActionUtils.discardFromDeckTop(targetBoard, count);
                if (discarded.isEmpty()) break;

                GameCard topCard = discarded.get(0);
                String subType = effect.getSubType();

                if (subType != null && subType.startsWith("attach_if:")) {
                    String requiredType = subType.substring("attach_if:".length());
                    if (topCard instanceof EnergyCard energy
                            && energy.getEnergyType().name().equalsIgnoreCase(requiredType)) {
                        targetBoard.getDiscardPile().remove(topCard);
                        if (ctx.getAttacker() != null) {
                            ctx.getAttacker().getAttachedEnergy().add(energy);
                            if (ctx.getPublisher() != null) {
                                ctx.getPublisher().publish(GameEventType.NOTIFICATION,
                                        ctx.getMatchId(), ctx.getPlayerId(),
                                        ctx.getSourceName() + ": attached " + energy.getName() + " from deck top",
                                        ctx.getSourceName(), null, 0);
                            }
                        }
                    }
                } else if (subType != null && subType.startsWith("damage_bonus:")) {
                    String[] parts = subType.split(":");
                    if (parts.length >= 3 && ctx.getDefender() != null) {
                        int bonus = Integer.parseInt(parts[1]);
                        String requiredType = parts[2];
                        if (topCard instanceof EnergyCard energy
                                && energy.getEnergyType().name().equalsIgnoreCase(requiredType)) {
                            ctx.getDefender().takeDamage(bonus);
                            if (ctx.getPublisher() != null) {
                                ctx.getPublisher().publish(GameEventType.NOTIFICATION,
                                        ctx.getMatchId(), ctx.getPlayerId(),
                                        ctx.getSourceName() + ": +" + bonus + " damage from " + energy.getName(),
                                        ctx.getSourceName(), null, 0);
                            }
                        }
                    }
                }
                break;
            }

            case MOVE_ENERGY:
                if (ctx.getAttacker() != null && ctx.getSourceBoard() != null
                        && !ctx.getSourceBoard().getBench().isEmpty()) {
                    List<EnergyCard> attached = ctx.getAttacker().getAttachedEnergy();
                    if (!attached.isEmpty()) {
                        EnergyCard movedEnergy = attached.remove(0);
                        PokemonInPlay benchTarget = ctx.getSourceBoard().getBench().get(0);
                        if (benchTarget != null) benchTarget.getAttachedEnergy().add(movedEnergy);
                    }
                }
                break;

            case REMOVE_CONDITION:
                if (ctx.getDefender() != null) {
                    ctx.getDefender().getSpecialConditions().clear();
                }
                break;

            case DECK_PEEK: {
                boolean isOpponent = effect.getTarget() == TargetScope.OPPONENT;
                PlayerBoard targetBoard = isOpponent ? ctx.getOpponentBoard() : ctx.getSourceBoard();
                if (targetBoard == null || targetBoard.getDeck().isEmpty()) break;

                List<GameCard> deck = targetBoard.getDeck();
                int count = Math.min(effect.getAmount() > 0 ? effect.getAmount() : 1, deck.size());
                List<String> names = new ArrayList<>();

                if (isOpponent) {
                    GameCard top = deck.get(0);
                    names.add(top.getName());
                } else {
                    Collections.shuffle(deck);
                    for (int i = 0; i < count; i++) {
                        names.add(deck.get(i).getName());
                    }
                }

                if (ctx.getPublisher() != null) {
                    String msg = ctx.getSourceName() + (isOpponent ? " peeked opponent's top card: " : " peeked: ")
                            + String.join(", ", names);
                    ctx.getPublisher().publish(GameEventType.NOTIFICATION,
                            ctx.getMatchId(), ctx.getPlayerId(), msg,
                            ctx.getSourceName(), null, 0);
                }

                Collections.shuffle(deck);
                break;
            }
        }
    }

    private int applyDefenderPrevention(PokemonInPlay defender, int incomingDamage, AttackContext context) {
        int totalPrevented = 0;
        if (defender == null || defender.getCard().getAbilities() == null) return 0;

        for (Ability ability : defender.getCard().getAbilities()) {
            if (ability.getType() != AbilityType.POKE_BODY) continue;
            List<AbilityEffect> effects = abilityResolver.resolve(ability);
            for (AbilityEffect effect : effects) {
                if (effect instanceof PreventDamageEffect pde) {
                    GameBoard board = context.getBoard();
                    String attackerId = context.getCurrentPlayerId();
                    PlayerBoard oppBoard = null;
                    PlayerBoard sourceBoard = null;
                    if (board != null) {
                        if (board.getPlayer1Board() != null && attackerId.equals(board.getPlayer1Board().getPlayerId())) {
                            oppBoard = board.getPlayer2Board();
                            sourceBoard = board.getPlayer1Board();
                        } else if (board.getPlayer2Board() != null) {
                            oppBoard = board.getPlayer1Board();
                            sourceBoard = board.getPlayer2Board();
                        }
                    }
                    AbilityEffectContext actx = new AbilityEffectContext(
                            context.getMatchId(), attackerId, defender,
                            board, sourceBoard, oppBoard, null, TurnPhase.ATTACK);
                    totalPrevented += pde.filterDamage(incomingDamage, actx);
                }
            }
        }
        return totalPrevented;
    }

    private SpecialCondition parseCondition(String subType) {
        if (subType == null) return null;
        return switch (subType.toUpperCase()) {
            case "POISONED", "POISON" -> SpecialCondition.POISONED;
            case "BURNED", "BURN" -> SpecialCondition.BURNED;
            case "ASLEEP", "SLEEP" -> SpecialCondition.ASLEEP;
            case "PARALYZED", "PARALY" -> SpecialCondition.PARALYZED;
            case "CONFUSED", "CONFUS" -> SpecialCondition.CONFUSED;
            default -> null;
        };
    }

    @Override
    public void setNext(AttackStep next) {
        this.next = next;
    }

    private void executeNext(AttackContext context) {
        if (next != null && context.isAttackSuccessful()) {
            next.execute(context);
        }
    }
}
