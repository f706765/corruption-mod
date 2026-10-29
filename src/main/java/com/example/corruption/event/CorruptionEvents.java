package com.example.corruption.event;

import com.example.corruption.network.CorruptionSyncPacket;
import com.example.corruption.registry.ModSounds;
import com.example.corruption.system.CorruptionSavedData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 腐化值事件调度（全部服务端逻辑）：
 * - 0-24 正常期：无事件（后续版本加极轻微环境音）
 * - 25+ 不安期：随机熄灭玩家附近一支火把
 * - 50+ 心理期：随机间隔播放低语（带距离衰减）
 */
public final class CorruptionEvents {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final int TORCH_SEARCH_RADIUS = 8;
    private static final double TORCH_CHANCE = 0.10D;     // 每秒 10% 概率尝试熄火把
    private static final double WHISPER_CHANCE = 0.06D;   // 每秒 6% 概率触发低语

    private CorruptionEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return; // 每秒一次，事件不需要逐 tick

        CorruptionSavedData data = CorruptionSavedData.get(server.overworld());
        long gameTime = server.overworld().getGameTime();
        boolean dirty = false;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            int corruption = data.get(player.getUUID(), gameTime);
            ServerLevel level = player.serverLevel();

            // 不安期：熄灭火把
            if (corruption >= 25 && RANDOM.nextDouble() < TORCH_CHANCE) {
                if (tryExtinguishTorch(level, player.blockPosition())) {
                    dirty = true;
                }
            }

            // 心理期：低语（可变距离音效，随机方位出现）
            if (corruption >= 50 && RANDOM.nextDouble() < WHISPER_CHANCE) {
                playWhisper(level, player);
            }

            // 把腐化值同步给客户端（驱动暗角/心跳/镜头特效）
            PacketDistributor.sendToPlayer(player, new CorruptionSyncPacket(corruption));
        }
        if (dirty) data.setDirty();
    }

    /** 在玩家周围 radius 内随机找一支火把并熄灭（直接移除）。 */
    private static boolean tryExtinguishTorch(ServerLevel level, BlockPos center) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = center.offset(
                    RANDOM.nextInt(TORCH_SEARCH_RADIUS * 2 + 1) - TORCH_SEARCH_RADIUS,
                    RANDOM.nextInt(5) - 2,
                    RANDOM.nextInt(TORCH_SEARCH_RADIUS * 2 + 1) - TORCH_SEARCH_RADIUS);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
                level.removeBlock(pos, false);
                level.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        4, 0.1D, 0.1D, 0.1D, 0.005D);
                return true;
            }
        }
        return false;
    }

    /** 低语：在玩家 6-10 格外的随机方位播放，音量低、衰减近——“好像就在耳边又不确定”。 */
    private static void playWhisper(ServerLevel level, ServerPlayer player) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dist = 6.0D + RANDOM.nextDouble() * 4.0D;
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;
        double y = player.getY() + (RANDOM.nextDouble() - 0.2D);
        level.playSound(null, x, y, z, ModSounds.WHISPER.value(),
                SoundSource.AMBIENT, 0.8F, 0.85F + RANDOM.nextFloat() * 0.3F);
    }

    /** /corruption get|set —— 测试指令，快速查看或拉高/清零腐化值。 */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("corruption")
                .then(Commands.literal("get").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    ServerLevel overworld = ctx.getSource().getServer().overworld();
                    int c = CorruptionSavedData.get(overworld).get(p.getUUID(), overworld.getGameTime());
                    ctx.getSource().sendSystemMessage(Component.literal("你的腐化值：" + c));
                    return 1;
                }))
                .then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) {
                                ctx.getSource().sendFailure(Component.literal("该指令只能由玩家执行"));
                                return 0;
                            }
                            int v = IntegerArgumentType.getInteger(ctx, "value");
                            ServerLevel overworld = ctx.getSource().getServer().overworld();
                            CorruptionSavedData.get(overworld).set(p.getUUID(), v, overworld.getGameTime());
                            PacketDistributor.sendToPlayer(p, new CorruptionSyncPacket(v));
                            ctx.getSource().sendSystemMessage(Component.literal("腐化值已设为 " + v));
                            return 1;
                        }))));
    }
}
