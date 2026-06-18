package wtf.fpp.pvp.combat;

import java.util.Random;
import java.util.UUID;

import org.bukkit.entity.LivingEntity;

import wtf.fpp.pvp.combat.ai.BotPersonality;
import wtf.fpp.pvp.combat.ai.CombatPhase;

// one bot mid-fight - brain reads this every tick
public final class PvpCombatSession {

    private final UUID botUuid;
    private final boolean once;
    private final BotPersonality personality;
    private final Random rng = new Random();

    private UUID lockedTarget;
    private LivingEntity currentTarget;

    private int ticksSinceSwing;
    private int ticksSinceDamaged;
    private int phaseTicksLeft;

    private CombatPhase phase = CombatPhase.APPROACH;

    private int strafeDir = 1;
    private int strafeFlipCooldown;

    private boolean targetWasBlocking;
    private int ticksSinceTargetWhiff;

    private UUID lastAttacker;

    public PvpCombatSession(UUID botUuid, boolean once, UUID lockedTarget) {
        this.botUuid = botUuid;
        this.once = once;
        this.lockedTarget = lockedTarget;
        this.personality = BotPersonality.roll(rng);
        this.strafeDir = rng.nextBoolean() ? 1 : -1;
    }

    public UUID botUuid() {
        return botUuid;
    }

    public boolean once() {
        return once;
    }

    public BotPersonality personality() {
        return personality;
    }

    public Random rng() {
        return rng;
    }

    public UUID lockedTarget() {
        return lockedTarget;
    }

    public void setLockedTarget(UUID lockedTarget) {
        this.lockedTarget = lockedTarget;
    }

    public LivingEntity currentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(LivingEntity currentTarget) {
        this.currentTarget = currentTarget;
    }

    public CombatPhase phase() {
        return phase;
    }

    public int phaseTicksLeft() {
        return phaseTicksLeft;
    }

    public double strafeDir() {
        return strafeDir;
    }

    public void tickPhase() {
        if (phaseTicksLeft > 0) {
            phaseTicksLeft--;
        }
        if (ticksSinceDamaged < 999) {
            ticksSinceDamaged++;
        }
        if (ticksSinceTargetWhiff < 999) {
            ticksSinceTargetWhiff++;
        }

        strafeFlipCooldown--;
        if (strafeFlipCooldown <= 0) {
            if (rng.nextDouble() < 0.08) {
                strafeDir *= -1;
            }
            strafeFlipCooldown = 20 + rng.nextInt(30);
        }
    }

    public void setPhase(CombatPhase phase, int ticks) {
        this.phase = phase;
        this.phaseTicksLeft = ticks;
    }

    public void resetAttackCooldown() {
        ticksSinceSwing = 0;
    }

    public void tickCooldown() {
        ticksSinceSwing++;
    }

    public boolean canAttack(int need) {
        return ticksSinceSwing >= need;
    }

    public int ticksSinceDamaged() {
        return ticksSinceDamaged;
    }

    public void noteDamaged(UUID attacker) {
        ticksSinceDamaged = 0;
        lastAttacker = attacker;
        if (lockedTarget == null && attacker != null) {
            lockedTarget = attacker;
        }
    }

    public UUID lastAttacker() {
        return lastAttacker;
    }

    public void updateTargetBlocking(boolean blocking) {
        if (targetWasBlocking && !blocking) {
            ticksSinceTargetWhiff = 0;
        }
        targetWasBlocking = blocking;
    }

    public boolean recentlyWhiffedByTarget() {
        return ticksSinceTargetWhiff < 10;
    }
}
