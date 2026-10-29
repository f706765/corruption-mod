package com.example.corruption.system;

import com.example.corruption.block.CursedTorchBlock;
import com.example.corruption.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 暗域房间：被长臂抓走后所在的漆黑密室。
 * - 在远处（基于玩家 UUID 哈希）生成一间全封闭的黑曜石房间，内置三个未点亮诅咒火炬。
 * - capture：建房间 + 给火把 + 短暂夜视 + 传送到房间中心。
 * - 三个火炬点亮（onTorchLit 校验）→ escape：传回原处并清理房间。
 */
public final class LongArmsRoom {
    public static final int OY = 60;

    private LongArmsRoom() {}

    public static BlockPos roomOrigin(UUID id) {
        long h = id.getLeastSignificantBits() ^ id.getMostSignificantBits();
        int ox = (int) Math.floorMod(h, 40000L) + 200000;
        int oz = (int) Math.floorMod(h >>> 16, 40000L) + 200000;
        return new BlockPos(ox, OY, oz);
    }

    public static List<BlockPos> anchors(BlockPos room) {
        List<BlockPos> a = new ArrayList<>();
        a.add(new BlockPos(room.getX() - 2, room.getY() + 1, room.getZ() - 2));
        a.add(new BlockPos(room.getX(),       room.getY() + 1, room.getZ() - 2));
        a.add(new BlockPos(room.getX() + 2, room.getY() + 1, room.getZ() - 2));
        return a;
    }

    public static void buildRoom(ServerLevel level, BlockPos room) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    BlockPos p = room.offset(dx, dy, dz);
                    boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                    if (dy == 0 || dy == 4 || wall) {
                        level.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 2);
                    } else {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (BlockPos a : anchors(room)) {
            level.setBlock(a, ModBlocks.CURSED_TORCH.get().defaultBlockState(), 2);
        }
    }

    public static void capture(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        UUID id = player.getUUID();
        LongArmsCapturedData data = LongArmsCapturedData.get(overworld);
        BlockPos room = roomOrigin(id);
        buildRoom(overworld, room);
        data.capture(id, player.getX(), player.getY(), player.getZ(),
                player.level().dimension().location().toString(),
                room.getX(), room.getY(), room.getZ());

        ItemStack torch = new ItemStack(Items.TORCH, 3);
        if (player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, torch);
        } else {
            player.getInventory().add(torch);
        }

        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0));

        player.teleportTo(overworld, room.getX() + 0.5D, room.getY() + 1.5D, room.getZ() + 0.5D,
                Set.of(), 0.0F, 0.0F);
    }

    public static void onTorchLit(ServerLevel level, ServerPlayer player) {
        LongArmsCapturedData data = LongArmsCapturedData.get(level);
        LongArmsCapturedData.Entry e = data.get(player.getUUID());
        if (e == null || !e.captured) return;
        BlockPos room = new BlockPos(e.ox, e.oy, e.oz);
        boolean all = true;
        for (BlockPos a : anchors(room)) {
            var st = level.getBlockState(a);
            if (!(st.getBlock() instanceof CursedTorchBlock) || !st.getValue(CursedTorchBlock.LIT)) {
                all = false;
                break;
            }
        }
        if (all) escape(player, e);
    }

    public static void escape(ServerPlayer player, LongArmsCapturedData.Entry e) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(e.dim));
        ServerLevel dest = player.server.getLevel(dim);
        if (dest == null) dest = player.server.overworld();
        player.teleportTo(dest, e.rx, e.ry, e.rz,
                Set.of(), player.getYRot(), player.getXRot());
        LongArmsCapturedData.get(player.server.overworld()).release(player.getUUID());
        clearRoom(player.server.overworld(), new BlockPos(e.ox, e.oy, e.oz));
    }

    private static void clearRoom(ServerLevel level, BlockPos room) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    level.setBlock(room.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }
}
