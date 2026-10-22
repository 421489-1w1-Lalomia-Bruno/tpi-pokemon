package ar.edu.utn.frc.tup.piii.engine.utils;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;

import java.util.Random;

public final class CoinFlipManager {

    private static Random RNG = new Random();

    private CoinFlipManager() {}

    /** For testing only: inject a seeded or fixed Random. Pass null to restore default. */
    public static void setRng(Random rng) {
        RNG = rng != null ? rng : new Random();
    }

    public static boolean flip() {
        return RNG.nextBoolean();
    }

    public static int flipHeads(int count) {
        int heads = 0;
        for (int i = 0; i < count; i++) {
            if (RNG.nextBoolean()) heads++;
        }
        return heads;
    }

    public static CoinFlipResult flip(int count) {
        int heads = flipHeads(count);
        return new CoinFlipResult(heads, count - heads, count);
    }

    public record CoinFlipResult(int heads, int tails, int total) {}

    public static void publishFlipResult(GameEventPublisher publisher, String matchId,
                                          String playerId, String sourceName,
                                          CoinFlipResult result) {
        if (publisher == null) return;
        publisher.publish(
            GameEventType.MULTI_COIN_FLIP, matchId, playerId,
            sourceName + ": " + result.heads() + "/" + result.total() + " heads, " + result.tails() + " tails",
            null, null, result.heads()
        );
    }
}
