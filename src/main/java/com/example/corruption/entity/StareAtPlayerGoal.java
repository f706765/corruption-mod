package com.example.corruption.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** 凝视：只要玩家在感知范围内，就一直“看”着玩家。 */
public class StareAtPlayerGoal extends Goal {
    private final WatcherEntity mob;
    private final double range;
    private Player target;

    public StareAtPlayerGoal(WatcherEntity mob, double range) {
        this.mob = mob;
        this.range = range;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player p = mob.level().getNearestPlayer(mob, range);
        if (p != null && !p.isCreative() && !p.isSpectator()) {
            target = p;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive()
                && !target.isCreative() && !target.isSpectator()
                && mob.distanceToSqr(target) < range * range;
    }

    @Override
    public void tick() {
        if (target != null) {
            mob.getLookControl().setLookAt(target.getX(),
                    target.getEyeY(), target.getZ());
        }
    }

    @Override
    public void stop() {
        target = null;
    }
}
