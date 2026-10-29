package com.example.corruption.entity;

import com.example.corruption.system.LongArmsRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 核心新实体 “长臂”（LongArms）：
 * - 全身漆黑、长臂长腿，蹲在树后只露出手。
 * - 玩家盯着它藏身的树超过 10 秒 → 察觉 → 冲过来把玩家抓进暗域。
 * - 抓取由服务端在 aiStep 内驱动，不依赖额外 Goal。
 */
public class LongArmsEntity extends PathfinderMob {
    private BlockPos lurkPos = null;
    private int stareTicks = 0;
    private Phase phase = Phase.LURK;
    private ServerPlayer target = null;

    private enum Phase { LURK, HUNT }

    public LongArmsEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)   // 仅在 HUNT 阶段用导航移动
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** 由生成逻辑（自然生成 / 刷怪蛋 / 指令）显式指定藏身的树。 */
    public void setLurk(BlockPos pos) { this.lurkPos = pos; }

    @Override
    protected void registerGoals() {}

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) this.level();

        if (phase == Phase.LURK) {
            if (lurkPos == null) findTree();
            Player nearest = null;
            double best = 48.0D * 48.0D;
            for (Player p : level.players()) {
                if (p.isSpectator()) continue;
                double d = p.distanceToSqr(this);
                if (d < best) { best = d; nearest = p; }
            }
            if (nearest instanceof ServerPlayer sp) {
                target = sp;
                HitResult hit = sp.pick(40.0D, 1.0F, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos bp = ((BlockHitResult) hit).getBlockPos();
                    BlockState bs = level.getBlockState(bp);
                    if (bs.is(BlockTags.LOGS) && lurkPos != null && bp.closerThan(lurkPos, 5.0D)) {
                        stareTicks++;
                    } else {
                        stareTicks = Math.max(0, stareTicks - 2);
                    }
                }
                if (stareTicks >= 200) detect(sp);   // 200 ticks = 10 秒
            }
        } else if (phase == Phase.HUNT) {
            if (target == null || !target.isAlive()) { this.discard(); return; }
            double d = this.distanceTo(target);
            if (d < 1.6D) {
                grab(target);
            } else {
                this.getNavigation().moveTo(target, 1.25D);
            }
        }
    }

    private void detect(ServerPlayer sp) {
        phase = Phase.HUNT;
        stareTicks = 0;
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(),
                    6, 0.3D, 0.3D, 0.3D, 0.02D);
        }
    }

    private void grab(ServerPlayer sp) {
        LongArmsRoom.capture(sp);
        this.discard();
    }

    private void findTree() {
        ServerLevel level = (ServerLevel) this.level();
        BlockPos center = this.blockPosition();
        for (int r = 2; r <= 10; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = -2; dy <= 4; dy++) {
                        BlockPos p = center.offset(dx, dy, dz);
                        if (level.getBlockState(p).is(BlockTags.LOGS)) {
                            this.lurkPos = p;
                            this.moveTo(p.getX() + 1.5D, p.getY() - 1.0D, p.getZ() + 0.5D, this.getYRot(), 0.0F);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }
}
