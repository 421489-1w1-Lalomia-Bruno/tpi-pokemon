package ar.edu.utn.frc.tup.piii.engine.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttackFlipResolverTest {

    private static final String FLIP_TEXT =
            "Flip 2 coins. This attack does 30 damage times the number of heads.";

    @Test
    void isFlipAttack_trueForCoinDamageText() {
        assertTrue(AttackFlipResolver.isFlipAttack(FLIP_TEXT));
    }

    @Test
    void isFlipAttack_falseForPlainOrNull() {
        assertFalse(AttackFlipResolver.isFlipAttack("This attack does 20 damage."));
        assertFalse(AttackFlipResolver.isFlipAttack(null));
    }

    @Test
    void resolve_damageWithinExpectedBounds() {
        for (int i = 0; i < 100; i++) {
            AttackFlipResolver.FlipResult r = AttackFlipResolver.resolve(FLIP_TEXT);
            assertEquals(2, r.getTotalCoins());
            assertTrue(r.getHeads() >= 0 && r.getHeads() <= 2);
            assertEquals(r.getHeads() * 30, r.getDamage());
        }
    }

    @Test
    void resolve_nullText_returnsZeroDamage() {
        AttackFlipResolver.FlipResult r = AttackFlipResolver.resolve(null);
        assertEquals(0, r.getDamage());
    }

    @Test
    void isConditionalFlipAttack_trueForFlipACoinWithIfHeads() {
        assertTrue(AttackFlipResolver.isConditionalFlipAttack(
                "Flip a coin. If heads, this attack does 30 more damage."));
    }

    @Test
    void isConditionalFlipAttack_trueForFlipACoinWithBothBranches() {
        assertTrue(AttackFlipResolver.isConditionalFlipAttack(
                "Flip a coin. If heads, the Defending Pokémon is now Paralyzed. If tails, this attack does nothing."));
    }

    @Test
    void isConditionalFlipAttack_falseForPlainText() {
        assertFalse(AttackFlipResolver.isConditionalFlipAttack("This attack does 20 damage."));
    }

    @Test
    void isConditionalFlipAttack_falseForNull() {
        assertFalse(AttackFlipResolver.isConditionalFlipAttack(null));
    }

    @Test
    void extractBranchText_headsWithTails_returnsHeadsBranch() {
        String result = AttackFlipResolver.extractBranchText(
                "Flip a coin. If heads,paralyze.If tails,nothing.", true);
        assertEquals(",paralyze.", result);
    }

    @Test
    void extractBranchText_tailsWithTails_returnsTailsBranch() {
        String result = AttackFlipResolver.extractBranchText(
                "Flip a coin. If heads,paralyze.If tails,nothing.", false);
        assertEquals(",nothing.", result);
    }

    @Test
    void extractBranchText_headsWithoutTails_returnsRemainingText() {
        String result = AttackFlipResolver.extractBranchText(
                "10+ Flip a coin. If heads,this attack does 30 more damage.", true);
        assertEquals(",this attack does 30 more damage.", result);
    }

    @Test
    void extractBranchText_tailsWithoutTails_returnsEmpty() {
        String result = AttackFlipResolver.extractBranchText(
                "10+ Flip a coin. If heads,this attack does 30 more damage.", false);
        assertEquals("", result);
    }
}
