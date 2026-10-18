package net.goldskinmc.creategeoresonance.seismic;

final class SeismicStationPenSpring {
    private static final double NATURAL_FREQUENCY = 0.36D;
    private static final double DAMPING = 0.12D;
    private static final double DAMPED_FREQUENCY = Math.sqrt(NATURAL_FREQUENCY * NATURAL_FREQUENCY - DAMPING * DAMPING);
    private static final double MAX_ANGLE_DEGREES = 7.5D;
    private static final double STRIKE_IMPULSE = 2.7D;
    private static final double ECHO_IMPULSE = 1.35D;
    private static final double REST_ENERGY = 1.0E-10D;

    private boolean initialized;
    private long lastTick;
    private double previousAngle;
    private double angle;
    private double velocity;

    void onStrike(long gameTime) {
        addImpulse(gameTime, STRIKE_IMPULSE);
    }

    void onEcho(long gameTime) {
        addImpulse(gameTime, ECHO_IMPULSE);
    }

    float angleRadians(long gameTime, float partialTicks) {
        advanceTo(gameTime);
        float partial = Math.max(0.0F, Math.min(1.0F, partialTicks));
        return (float) Math.toRadians(previousAngle + (angle - previousAngle) * partial);
    }

    private void addImpulse(long gameTime, double impulse) {
        advanceTo(gameTime);
        // Bound spring energy without moving the weight or resetting its interpolation.
        double maxVelocity = NATURAL_FREQUENCY
            * Math.sqrt(Math.max(0.0D, MAX_ANGLE_DEGREES * MAX_ANGLE_DEGREES - angle * angle));
        velocity = Math.max(-maxVelocity, Math.min(maxVelocity, velocity + impulse));
    }

    private void advanceTo(long gameTime) {
        if (!initialized) {
            initialized = true;
            lastTick = gameTime;
            return;
        }
        long elapsed = gameTime - lastTick;
        if (elapsed <= 0) {
            return;
        }

        // Solve the damped oscillator directly, including time spent outside the camera view.
        previousAngle = angleAfter(elapsed - 1.0D);
        double nextAngle = angleAfter(elapsed);
        double phase = DAMPED_FREQUENCY * elapsed;
        velocity = Math.exp(-DAMPING * elapsed) * (velocity * Math.cos(phase)
            - (DAMPING * velocity + NATURAL_FREQUENCY * NATURAL_FREQUENCY * angle)
                / DAMPED_FREQUENCY * Math.sin(phase));
        angle = nextAngle;
        lastTick = gameTime;

        double energy = angle * angle + velocity * velocity / (NATURAL_FREQUENCY * NATURAL_FREQUENCY);
        if (energy < REST_ENERGY) {
            angle = 0.0D;
            velocity = 0.0D;
        }
    }

    private double angleAfter(double ticks) {
        double phase = DAMPED_FREQUENCY * ticks;
        return Math.exp(-DAMPING * ticks) * (angle * Math.cos(phase)
            + (velocity + DAMPING * angle) / DAMPED_FREQUENCY * Math.sin(phase));
    }
}
