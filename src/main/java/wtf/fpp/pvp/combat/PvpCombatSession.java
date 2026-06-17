package wtf.fpp.pvp.combat;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;

public final class PvpCombatSession {

    private final UUID botUuid;
    private final boolean once;
    private UUID lockedTarget;
    private LivingEntity currentTarget; // might use later for gui idk
    private int ticksSinceSwing;

    public PvpCombatSession(UUID botUuid, boolean once, UUID lockedTarget) {
        this.botUuid = botUuid;
        this.once = once;
        this.lockedTarget = lockedTarget;
    }

    public UUID botUuid() {
        return botUuid;
    }

    public boolean once() {
        return once;
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

    public void resetAttackCooldown() {
        ticksSinceSwing = 0;
    }

    public void tickCooldown() {
        ticksSinceSwing++;
    }

    public boolean canAttack(int need) {
        return ticksSinceSwing >= need;
    }
}
