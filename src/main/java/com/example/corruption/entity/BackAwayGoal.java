package com.example.corruption.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** 后退：玩家靠近（< trigger 距离）时，沿远离玩家的方向缓慢移动。 */
public class BackAwayGoal extends Goal {
    private final WatcherEntity mob;
    private final double triggerDistance;
    private final double speed;
    private Player threat;

    public BackAwayGoal(WatcherEntity mob, double triggerDistance, double speed) {
        this.mob = mob;
        this.triggerDistance = triggerDistance;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        Player p = mob.level().getNearestPlayer(mob, triggerDistance);
        if (p != null && !p.isCreative() && !p.isSpectator()) {
            threat = p;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return threat != null && threat.isAlive()
                && mob.distanceToSqr(threat) < triggerDistance * triggerDistance * 2.0D;
    }

    @Override
    public void start() {
        retreat();
    }

    @Override
    public void tick() {
        if (threat == null) return;
        // 玩家继续逼近就持续后退，保持“贴着极限距离”的不安感
        if (mob.distanceToSqr(threat) < triggerDistance * triggerDistance) {
            retreat();
        }
    }

    private void retreat() {
        Vec3 away = mob.position().subtract(threat.position());
        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(1, 0, 0); // 极端重叠时随便选个方向
        }
        away = away.normalize().scale(6.0D);
        mob.getNavigation().moveTo(
                mob.getX() + away.x,
                mob.getY(),
                mob.getZ() + away.z,
                speed);
    }

    @Override
    public void stop() {
        threat = null;
        mob.getNavigation().stop();
    }
}
