package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameActionUtils {

    private GameActionUtils() {}

    public static int drawCards(PlayerBoard board, int count,
                                 boolean discardHandFirst, boolean shuffleHandFirst,
                                 GameEventPublisher publisher, String matchId,
                                 String playerId, String sourceName) {
        List<GameCard> hand = board.getHand();
        List<GameCard> deck = board.getDeck();

        if (discardHandFirst) {
            board.getDiscardPile().addAll(hand);
            hand.clear();
        } else if (shuffleHandFirst) {
            deck.addAll(hand);
            hand.clear();
            Collections.shuffle(deck);
        }

        int drawn = 0;
        while (drawn < count && !deck.isEmpty()) {
            hand.add(deck.remove(0));
            drawn++;
        }

        if (drawn > 0 && publisher != null) {
            publisher.publish(GameEventType.CARD_DRAWN, matchId, playerId,
                    sourceName + ": drew " + drawn + " card" + (drawn > 1 ? "s" : ""));
        }
        return drawn;
    }

    public static int drawCards(PlayerBoard board, int count,
                                 GameEventPublisher publisher, String matchId,
                                 String playerId, String sourceName) {
        return drawCards(board, count, false, false, publisher, matchId, playerId, sourceName);
    }

    public static int healDamage(PokemonInPlay target, int amount) {
        int maxHp = target.getCard().getHp();
        int oldHp = target.getCurrentHp();
        int newHp = Math.min(maxHp, oldHp + amount);
        int healed = newHp - oldHp;
        target.setCurrentHp(newHp);
        return healed;
    }

    public static int healDamage(PokemonInPlay target, int amount,
                                  boolean discardEnergyAfter, PlayerBoard board,
                                  GameEventPublisher publisher, String matchId,
                                  String playerId, String sourceName) {
        int healed = healDamage(target, amount);
        if (healed > 0 && publisher != null) {
            publisher.publish(GameEventType.PHASE_CHANGED, matchId, playerId,
                    sourceName + ": healed " + healed + " damage from " + target.getCard().getName());
        }
        if (discardEnergyAfter && !target.getAttachedEnergy().isEmpty()) {
            EnergyCard discarded = target.getAttachedEnergy().remove(0);
            board.getDiscardPile().add(discarded);
        }
        return healed;
    }

    public static List<GameCard> searchDeck(PlayerBoard board, String targetType,
                                             boolean basicOnly, int maxResults) {
        List<GameCard> deck = board.getDeck();
        List<GameCard> matches = new ArrayList<>();
        for (GameCard card : deck) {
            if (matches.size() >= maxResults) break;
            if (targetType == null) {
                matches.add(card);
            } else if ("pokemon".equals(targetType)) {
                if (card instanceof PokemonCard pokemon) {
                    if (!basicOnly || pokemon.getStage() == PokemonStage.BASIC) {
                        matches.add(card);
                    }
                }
            } else if ("energy".equals(targetType)) {
                if (card instanceof EnergyCard energy) {
                    if (!basicOnly || !energy.isSpecial()) {
                        matches.add(card);
                    }
                }
            } else if ("trainer".equals(targetType)) {
                if (card instanceof TrainerCard) {
                    matches.add(card);
                }
            }
        }
        for (GameCard match : matches) {
            deck.remove(match);
            board.getHand().add(match);
        }
        Collections.shuffle(deck);
        return matches;
    }

    public static boolean switchActive(PlayerBoard board, String targetId) {
        PokemonInPlay active = board.getActivePokemon();
        if (active == null) return false;

        PokemonInPlay toSwap = null;
        if (targetId != null) {
            for (PokemonInPlay bench : board.getBench()) {
                if (bench.getCard().getId().equals(targetId)) {
                    toSwap = bench;
                    break;
                }
            }
        }
        if (toSwap == null && !board.getBench().isEmpty()) {
            toSwap = board.getBench().get(0);
        }
        if (toSwap == null) return false;

        board.getBench().remove(toSwap);
        board.getBench().add(active);
        active.setActive(false);
        active.getSpecialConditions().clear();
        board.setActivePokemon(toSwap);
        toSwap.setActive(true);
        toSwap.getSpecialConditions().clear();
        return true;
    }

    public static boolean switchActive(PlayerBoard board, String targetId,
                                        GameEventPublisher publisher, String matchId,
                                        String playerId, String sourceName) {
        PokemonInPlay active = board.getActivePokemon();
        if (!switchActive(board, targetId)) return false;
        PokemonInPlay toSwap = board.getActivePokemon();
        if (publisher != null) {
            publisher.publish(GameEventType.POKEMON_RETREATED, matchId, playerId,
                    sourceName + ": switched " + active.getCard().getName()
                            + " with " + toSwap.getCard().getName());
        }
        return true;
    }

    public static EnergyCard discardEnergy(PokemonInPlay target, boolean specialOnly,
                                             PlayerBoard targetBoard) {
        EnergyCard toDiscard = null;
        for (EnergyCard e : target.getAttachedEnergy()) {
            if (!specialOnly || e.isSpecial()) {
                toDiscard = e;
                break;
            }
        }
        if (toDiscard == null) return null;
        target.getAttachedEnergy().remove(toDiscard);
        targetBoard.getDiscardPile().add(toDiscard);
        return toDiscard;
    }

    public static EnergyCard discardEnergy(PokemonInPlay target, boolean specialOnly,
                                             PlayerBoard targetBoard,
                                             GameEventPublisher publisher, String matchId,
                                             String playerId, String sourceName) {
        EnergyCard toDiscard = discardEnergy(target, specialOnly, targetBoard);
        if (toDiscard != null && publisher != null) {
            publisher.publish(GameEventType.PHASE_CHANGED, matchId, playerId,
                    sourceName + ": discarded " + toDiscard.getName()
                            + " from " + target.getCard().getName());
        }
        return toDiscard;
    }

    public static int redCardEffect(PlayerBoard opponent, int cardsToDraw,
                                      GameEventPublisher publisher, String matchId,
                                      String playerId, String sourceName) {
        List<GameCard> hand = opponent.getHand();
        List<GameCard> deck = opponent.getDeck();
        
        if (!hand.isEmpty()) {
            deck.addAll(hand);
            hand.clear();
        }
        Collections.shuffle(deck);
        
        int drawn = 0;
        while (drawn < cardsToDraw && !deck.isEmpty()) {
            hand.add(deck.remove(0));
            drawn++;
        }
        
        if (publisher != null) {
            publisher.publish(GameEventType.CARD_DRAWN, matchId, playerId,
                    sourceName + ": opponent reshuffled their hand and drew "
                            + drawn + " card" + (drawn == 1 ? "" : "s"));
        }
        return drawn;
    }

    public static void discardCards(PlayerBoard board, int count,
                                    GameEventPublisher publisher, String matchId,
                                    String playerId, String sourceName) {
        List<GameCard> hand = board.getHand();
        int discarded = 0;
        while (discarded < count && !hand.isEmpty()) {
            board.getDiscardPile().add(hand.remove(0));
            discarded++;
        }
        if (publisher != null) {
            publisher.publish(GameEventType.PHASE_CHANGED, matchId, playerId,
                    sourceName + ": discarded " + discarded + " cards from hand");
        }
    }

    public static void shuffleHandIntoDeck(PlayerBoard board,
                                           GameEventPublisher publisher, String matchId,
                                           String playerId, String sourceName) {
        board.getDeck().addAll(board.getHand());
        board.getHand().clear();
        Collections.shuffle(board.getDeck());
        if (publisher != null) {
            publisher.publish(GameEventType.PHASE_CHANGED, matchId, playerId,
                    sourceName + ": shuffled hand into deck");
        }
    }

    public static void shuffleDeck(PlayerBoard board,
                                   GameEventPublisher publisher, String matchId,
                                   String playerId, String sourceName) {
        Collections.shuffle(board.getDeck());
        if (publisher != null) {
            publisher.publish(GameEventType.PHASE_CHANGED, matchId, playerId,
                    sourceName + ": shuffled deck");
        }
    }

    public static List<GameCard> retrieveFromDiscard(PlayerBoard board, String targetType,
                                                       boolean basicOnly, int maxResults,
                                                       String destination, boolean deckTop) {

        List<GameCard> discard = board.getDiscardPile();
        List<GameCard> matches = new ArrayList<>();

        for (GameCard card : discard) {
            if (matches.size() >= maxResults) break;
            if (targetType == null) {
                matches.add(card);
            } else if ("pokemon".equals(targetType)) {
                if (card instanceof PokemonCard pokemon) {
                    if (!basicOnly || pokemon.getStage() == PokemonStage.BASIC) {
                        matches.add(card);
                    }
                }
            } else if ("energy".equals(targetType)) {
                if (card instanceof EnergyCard energy) {
                    if (!basicOnly || !energy.isSpecial()) {
                        matches.add(card);
                    }
                }
            } else if ("trainer".equals(targetType)) {
                if (card instanceof TrainerCard) {
                    matches.add(card);
                }
            }
        }

        for (GameCard match : matches) {
            discard.remove(match);
        }

        if ("deck".equals(destination)) {
            if (deckTop) {
                board.getDeck().addAll(0, matches);
            } else {
                board.getDeck().addAll(matches);
                Collections.shuffle(board.getDeck());
            }
        } else {
            board.getHand().addAll(matches);
        }
        return matches;
    }

    public static List<GameCard> discardFromDeckTop(PlayerBoard board, int count) {
        List<GameCard> deck = board.getDeck();
        List<GameCard> discarded = new ArrayList<>();
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            GameCard card = deck.remove(0);
            board.getDiscardPile().add(card);
            discarded.add(card);
        }
        return discarded;
    }

    public static boolean flipCoinApplyStatus(GameEventPublisher publisher, String matchId,
                                               String playerId, String sourceName,
                                               PokemonInPlay target,
                                               ar.edu.utn.frc.tup.piii.enums.SpecialCondition condition,
                                               ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager manager) {
        boolean heads = ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager.flip();
        if (publisher != null) {
            publisher.publish(GameEventType.COIN_FLIP_RESULT, matchId, playerId,
                    sourceName + ": " + (heads ? "Heads" : "Tails"),
                    sourceName, null, heads ? 1 : 0);
        }
        if (heads && target != null) {
            manager.addEffect(target, condition);
            return true;
        }
        return false;
    }

    public static String findPokemonName(GameCard card) {
        if (card == null) return "unknown";
        return card.getName();
    }
}
