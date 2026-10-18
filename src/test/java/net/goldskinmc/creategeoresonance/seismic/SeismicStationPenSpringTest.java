package net.goldskinmc.creategeoresonance.seismic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeismicStationPenSpringTest {
    @Test
    void overlappingEchoPreservesTheInterpolatedPoseButChangesSubsequentMotion() {
        SeismicStationPenSpring spring = new SeismicStationPenSpring();
        SeismicStationPenSpring strikeOnly = new SeismicStationPenSpring();
        spring.onStrike(0);
        strikeOnly.onStrike(0);
        float before = spring.angleRadians(4, 0.75F);

        spring.onEcho(4);

        assertEquals(before, spring.angleRadians(4, 0.75F), 1.0E-7F);
        assertNotEquals(strikeOnly.angleRadians(5, 0.5F), spring.angleRadians(5, 0.5F), 1.0E-5F);
    }

    @Test
    void overlappingStrikeDoesNotResetAnEchoMovingBackTowardTheCenter() {
        SeismicStationPenSpring spring = new SeismicStationPenSpring();
        spring.onEcho(0);
        float before = spring.angleRadians(8, 0.5F);

        spring.onStrike(8);

        assertEquals(before, spring.angleRadians(8, 0.5F), 1.0E-7F);
    }

    @Test
    void strikeReactionIsStrongerThanEchoReaction() {
        SeismicStationPenSpring strike = new SeismicStationPenSpring();
        SeismicStationPenSpring echo = new SeismicStationPenSpring();
        strike.onStrike(0);
        echo.onEcho(0);

        assertEquals(echo.angleRadians(4, 0.5F) * 2.0F, strike.angleRadians(4, 0.5F), 1.0E-7F);
        assertTrue(echo.angleRadians(4, 0.5F) > 0.0F);
    }

    @Test
    void repeatedAndSimultaneousImpulsesRemainWithinTheSwingLimit() {
        SeismicStationPenSpring spring = new SeismicStationPenSpring();
        double limit = Math.toRadians(7.5D) + 1.0E-6D;

        for (int tick = 0; tick < 500; tick++) {
            for (int impulse = 0; impulse < 10; impulse++) {
                spring.onStrike(tick);
                spring.onEcho(tick);
            }
            for (float partial : new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F}) {
                float angle = spring.angleRadians(tick, partial);
                assertTrue(Float.isFinite(angle));
                assertTrue(Math.abs(angle) <= limit);
            }
        }
    }

    @Test
    void skippedTicksMatchContinuousRendering() {
        SeismicStationPenSpring continuous = new SeismicStationPenSpring();
        SeismicStationPenSpring skipped = new SeismicStationPenSpring();
        continuous.onStrike(0);
        skipped.onStrike(0);

        for (int tick = 1; tick <= 12; tick++) {
            continuous.angleRadians(tick, 0.5F);
        }

        assertEquals(continuous.angleRadians(12, 0.5F), skipped.angleRadians(12, 0.5F), 1.0E-7F);
        continuous.onEcho(12);
        skipped.onEcho(12);
        for (int tick = 13; tick <= 20; tick++) {
            continuous.angleRadians(tick, 0.5F);
        }
        assertEquals(continuous.angleRadians(20, 0.5F), skipped.angleRadians(20, 0.5F), 1.0E-7F);
    }

    @Test
    void motionSettlesAndCanBeExcitedAgainAfterALongGap() {
        SeismicStationPenSpring spring = new SeismicStationPenSpring();
        assertEquals(0.0F, spring.angleRadians(0, 0.5F));
        spring.onStrike(0);
        assertTrue(spring.angleRadians(4, 0.5F) > 0.0F);

        assertEquals(0.0F, spring.angleRadians(200, 0.5F), 1.0E-6F);
        spring.onEcho(200);
        assertTrue(spring.angleRadians(204, 0.5F) > 0.0F);
    }
}
