package com.example.corruption.event;

import com.example.corruption.entity.LongArmsEntity;
import com.example.corruption.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 长臂的自然生成调度：每 60 秒，对每位处于森林/针叶林生物群系、夜晚、主世界的玩家，
 * 若附近没有长臂，则在附近找一棵树并生成一只长臂（蹲在树后）。
 */
public final class LongArmsEvents {
    private static final Map<UUID, Long> nextSpawn = new HashMap<>();

    private LongArmsEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 1200 != 0) return;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerLevel level = p.serverLevel();
            if (level.dimension() != Level.OVERWORLD) continue;

            long t = level.getDayTime() % 24000;
            if (t < 13000 || t > 23000) continue;   // 仅夜晚

            var biome = level.getBiome(p.blockPosition());
            if (!biome.is(BiomeTags.IS_FOREST) && !biome.is(BiomeTags.IS_TAIGA)) continue;

            long cd = nextSpawn.getOrDefault(p.getUUID(), 0L);
            if (cd > server.getTickCount()) continue;

            if (countNear(level, p.blockPosition(), 80) > 0) continue;

            BlockPos tree = findTree(level, p.blockPosition());
            if (tree == null) continue;

            LongArmsEntity ent = ModEntities.LONG_ARMS.get().create(level);
            if (ent == null) continue;
            ent.moveTo(tree.getX() + 1.5D, tree.getY() - 1.0D, tree.getZ() + 0.5D, 0.0F, 0.0F);
            ent.setLurk(tree);
            level.addFreshEntity(ent);
            nextSpawn.put(p.getUUID(), server.getTickCount() + 6000L);   // 冷却 5 分钟
        }
    }

    private static int countNear(ServerLevel level, BlockPos center, int r) {
        AABB box = AABB.ofSize(new Vec3(center.getX(), center.getY(), center.getZ()), r * 2, r * 2, r * 2);
        return level.getEntitiesOfClass(LongArmsEntity.class, box).size();
    }

    private static BlockPos findTree(ServerLevel level, BlockPos center) {
        for (int r = 4; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = -2; dy <= 6; dy++) {
                        BlockPos p = center.offset(dx, dy, dz);
                        if (level.getBlockState(p).is(BlockTags.LOGS)) return p;
                    }
                }
            }
        }
        return null;
    }
}
