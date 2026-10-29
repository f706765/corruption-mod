package com.example.corruption.entity;

import com.example.corruption.system.CorruptionSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 核心实体 “凝视者”（v1 只做形态 B）：
 * - 玩家靠近它后退，玩家停下/远离它就注视玩家（你追它跑，你停它看）。
 * - 不主动攻击；腐化值 95+ 后才进入攻击模式（后续版本）。
 */
public class WatcherEntity extends PathfinderMob {
    public WatcherEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // 不自然消失：它不该被常规 despawn 掉
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BackAwayGoal(this, 6.0D, 1.0D));
        this.goalSelector.addGoal(1, new StareAtPlayerGoal(this, 32.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.tickCount % 100 == 0) {
            // 终局阶段（腐化 95+）在这里切入攻击 Goal；v1 留空
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        // 玩家腐化值归零后允许消失；当前简单处理：远离 128 格才消失
        return dist > 128.0D;
    }

    /** 读取最近玩家腐化值（服务端） */
    public int corruptionOf(Player player) {
        if (player.level() instanceof ServerLevel server) {
            return CorruptionSavedData.get(server).get(player.getUUID(), server.getGameTime());
        }
        return 0;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
