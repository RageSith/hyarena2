package de.ragesith.hyarena2.bot;

import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.participant.Participant;

import java.util.Random;
import java.util.UUID;

/**
 * AI state machine for bot behavior.
 * Handles state transitions: IDLE -> PATROL -> CHASE -> ATTACK -> RETREAT -> DEAD
 */
public class BotAI {

    /**
     * AI states.
     */
    public enum State {
        IDLE,       // Scanning for targets
        PATROL,     // Moving to random arena points
        CHASE,      // Moving toward target in range
        ATTACK,     // In melee range, dealing damage
        RETREAT,    // Low health, moving away
        DEAD        // Waiting for respawn/removal
    }

    private final BotParticipant bot;
    private final BotDifficulty difficulty;
    private final Random random;

    private State currentState;
    private UUID currentTargetId;
    private Position currentTargetPosition;
    private long stateEnterTime;
    private long lastReactionTime;

    // Callback for when bot deals damage
    private BotDamageCallback damageCallback;

    public BotAI(BotParticipant bot) {
        this.bot = bot;
        this.difficulty = bot.getDifficulty();
        this.random = new Random();
        this.currentState = State.IDLE;
        this.stateEnterTime = System.currentTimeMillis();
        this.lastReactionTime = 0;
    }

    /**
     * Callback interface for bot damage events.
     */
    @FunctionalInterface
    public interface BotDamageCallback {
        void onBotDamage(BotParticipant attacker, UUID victimId, double damage);
    }

    /**
     * Sets the callback for when this bot deals damage.
     */
    public void setDamageCallback(BotDamageCallback callback) {
        this.damageCallback = callback;
    }

    /**
     * Gets the current AI state.
     */
    public State getCurrentState() {
        return currentState;
    }

    /**
     * Gets the current target ID.
     */
    public UUID getCurrentTargetId() {
        return currentTargetId;
    }

    /**
     * Sets the current target.
     */
    public void setTarget(UUID targetId, Position targetPosition) {
        this.currentTargetId = targetId;
        this.currentTargetPosition = targetPosition;
    }

    /**
     * Clears the current target.
     */
    public void clearTarget() {
        this.currentTargetId = null;
        this.currentTargetPosition = null;
    }

    /**
     * Updates the target position.
     */
    public void updateTargetPosition(Position position) {
        this.currentTargetPosition = position;
    }

    /**
     * Main tick method - updates AI state and executes behavior.
     * @return true if a state transition occurred
     */
    public boolean tick() {
        if (!bot.isAlive()) {
            if (currentState != State.DEAD) {
                transitionTo(State.DEAD);
                return true;
            }
            return false;
        }

        // Check reaction time delay before making decisions
        long now = System.currentTimeMillis();
        if (now - lastReactionTime < difficulty.getReactionTimeMs()) {
            return false;
        }
        lastReactionTime = now;

        // Evaluate state transitions
        State newState = evaluateNextState();
        if (newState != currentState) {
            transitionTo(newState);
            return true;
        }

        // Execute current state behavior
        executeStateBehavior();
        return false;
    }

    /**
     * Evaluates what state the bot should transition to.
     */
    private State evaluateNextState() {
        // If dead, stay dead
        if (!bot.isAlive()) {
            return State.DEAD;
        }

        // Check retreat condition (low health)
        if (shouldRetreat()) {
            return State.RETREAT;
        }

        // If we have no target, idle or patrol
        if (currentTargetId == null || currentTargetPosition == null) {
            return State.IDLE;
        }

        // Calculate distance to target
        double distance = calculateDistanceToTarget();

        // Within attack range -> attack
        if (distance <= difficulty.getAttackRange()) {
            return State.ATTACK;
        }

        // Within chase range -> chase
        if (distance <= difficulty.getChaseRange()) {
            return State.CHASE;
        }

        // Target too far, go back to idle/patrol
        return State.PATROL;
    }

    /**
     * Checks if the bot should retreat (low health).
     */
    private boolean shouldRetreat() {
        return bot.getHealthPercentage() <= difficulty.getRetreatThreshold();
    }

    /**
     * Calculates distance to current target.
     */
    private double calculateDistanceToTarget() {
        Position botPos = bot.getCurrentPosition();
        if (botPos == null || currentTargetPosition == null) {
            return Double.MAX_VALUE;
        }
        return botPos.distanceTo(currentTargetPosition);
    }

    /**
     * Transitions to a new state.
     */
    private void transitionTo(State newState) {
        State oldState = currentState;
        currentState = newState;
        stateEnterTime = System.currentTimeMillis();

        // State exit logic
        onStateExit(oldState);

        // State enter logic
        onStateEnter(newState);
    }

    /**
     * Called when exiting a state.
     */
    private void onStateExit(State state) {
        // Nothing specific needed for now
    }

    /**
     * Called when entering a state.
     */
    private void onStateEnter(State state) {
        switch (state) {
            case DEAD:
                clearTarget();
                break;
            case RETREAT:
                // Could trigger retreat animation/behavior
                break;
            default:
                break;
        }
    }

    /**
     * Executes behavior for the current state.
     */
    private void executeStateBehavior() {
        switch (currentState) {
            case IDLE:
                executeIdle();
                break;
            case PATROL:
                executePatrol();
                break;
            case CHASE:
                executeChase();
                break;
            case ATTACK:
                executeAttack();
                break;
            case RETREAT:
                executeRetreat();
                break;
            case DEAD:
                // Do nothing
                break;
        }
    }

    /**
     * IDLE behavior - scanning for targets.
     * NPC handles actual targeting via BotManager.
     */
    private void executeIdle() {
        // Transition to patrol after a short idle period
        long idleTime = System.currentTimeMillis() - stateEnterTime;
        if (idleTime > 2000) { // 2 seconds
            // Let tick() handle transition
        }
    }

    /**
     * PATROL behavior - moving to random points.
     * NPC handles actual movement.
     */
    private void executePatrol() {
        // NPC will handle movement to target once set
    }

    /**
     * CHASE behavior - moving toward target.
     * NPC handles actual movement via target system.
     */
    private void executeChase() {
        // NPC system handles chasing via MarkedEntitySupport
    }

    /**
     * ATTACK behavior - dealing damage when in range.
     */
    private void executeAttack() {
        if (!bot.canAttack()) {
            return;
        }

        if (currentTargetId == null) {
            return;
        }

        // Check if attack hits (based on accuracy)
        if (random.nextDouble() <= difficulty.getAimAccuracy()) {
            // Calculate damage (base damage from kit or default)
            double baseDamage = 10.0; // Default melee damage

            // Notify via callback (handleBotDamage tracks damage dealt)
            if (damageCallback != null) {
                damageCallback.onBotDamage(bot, currentTargetId, baseDamage);
            }

            bot.setLastAttackTime(System.currentTimeMillis());
        } else {
            // Miss - still update attack time
            bot.setLastAttackTime(System.currentTimeMillis());
        }
    }

    /**
     * RETREAT behavior - moving away from threats.
     */
    private void executeRetreat() {
        // NPC system doesn't have native retreat, but we can
        // clear target to stop chasing
        if (currentTargetId != null && shouldRetreat()) {
            clearTarget();
        }

        // If health recovers above threshold, go back to chase
        if (!shouldRetreat() && currentTargetId != null) {
            // State transition will be handled by tick()
        }
    }

    /**
     * Resets the AI state (for respawn).
     */
    public void reset() {
        currentState = State.IDLE;
        currentTargetId = null;
        currentTargetPosition = null;
        stateEnterTime = System.currentTimeMillis();
        lastReactionTime = 0;
    }

    /**
     * Gets time spent in current state (milliseconds).
     */
    public long getTimeInCurrentState() {
        return System.currentTimeMillis() - stateEnterTime;
    }
}
