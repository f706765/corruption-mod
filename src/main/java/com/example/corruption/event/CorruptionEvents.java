package com.example.corruption.event;

import com.example.corruption.entity.CorruptedCorpseEntity;
import com.example.corruption.entity.LongArmsEntity;
import com.example.corruption.network.CorruptionSyncPacket;
import com.example.corruption.registry.ModEffects;
import com.example.corruption.registry.ModEntities;
import com.example.corruption.registry.ModSounds;
import com.example.corruption.system.CorruptionSavedData;
import com.example.corruption.system.LongArmsCapturedData;
import com.example.corruption.system.LongArmsRoom;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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

            // 极重度腐化：极小概率被“感染”而开始流血
            if (corruption >= 90 && !player.hasEffect(ModEffects.BLEEDING) && RANDOM.nextDouble() < 0.02D) {
                player.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 600, 0));
            }

            // 把腐化值同步给客户端（驱动暗角/心跳/镜头特效）
            PacketDistributor.sendToPlayer(player, new CorruptionSyncPacket(corruption));
        }
        if (dirty) data.setDirty();
    }

    /**
     * 玩家死亡时：若腐化较高，原来的躯体被腐化操控，在原地留下一具腐化尸体。
     * 同时把玩家腐化清零，使其复活后不会被立刻锁成傀儡。
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.serverLevel();
        ServerLevel ow = sp.getServer().overworld();
        int c = CorruptionSavedData.get(ow).get(sp.getUUID(), level.getGameTime());
        if (c < 25) return;

        CorruptedCorpseEntity corpse = ModEntities.CORRUPTED_CORPSE.get().create(level);
        if (corpse != null) {
            corpse.moveTo(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), 0.0F);
            corpse.setSevere(c >= 80);
            level.addFreshEntity(corpse);
        }
        CorruptionSavedData.get(ow).set(sp.getUUID(), 0, ow.getGameTime());
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
                                })))
                .then(Commands.literal("corpse").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    ServerLevel level = p.serverLevel();
                    CorruptedCorpseEntity e = ModEntities.CORRUPTED_CORPSE.get().create(level);
                    if (e != null) {
                        e.moveTo(p.getX() + 2.0D, p.getY(), p.getZ(), 0.0F, 0.0F);
                        e.setSevere(true);
                        level.addFreshEntity(e);
                        ctx.getSource().sendSystemMessage(Component.literal("已在你身边生成一具腐化尸体（重度），用布+铅块包裹后用铲子埋掉"));
                    }
                    return 1;
                }))
                .then(Commands.literal("bleed").then(Commands.literal("on").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    p.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 600, 0));
                    ctx.getSource().sendSystemMessage(Component.literal("已为你附加流血效果"));
                    return 1;
                })).then(Commands.literal("off").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    p.removeEffect(ModEffects.BLEEDING);
                    ctx.getSource().sendSystemMessage(Component.literal("已解除流血效果"));
                    return 1;
                }))));

        // /longarms <spawn|capture|escape> —— 长臂机制测试指令
        event.getDispatcher().register(Commands.literal("longarms")
                .then(Commands.literal("spawn").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    ServerLevel level = p.serverLevel();
                    LongArmsEntity ent = ModEntities.LONG_ARMS.get().create(level);
                    if (ent != null) {
                        ent.moveTo(p.getX() + 3.0D, p.getY(), p.getZ(), 0.0F, 0.0F);
                        level.addFreshEntity(ent);
                        ctx.getSource().sendSystemMessage(Component.literal("已在你身边生成长臂"));
                    }
                    return 1;
                }))
                .then(Commands.literal("capture").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    LongArmsRoom.capture(p);
                    ctx.getSource().sendSystemMessage(Component.literal("你被抓进了暗域，点亮三个火炬即可逃脱"));
                    return 1;
                }))
                .then(Commands.literal("escape").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    LongArmsCapturedData.Entry e = LongArmsCapturedData.get(p.server.overworld()).get(p.getUUID());
                    if (e == null) {
                        ctx.getSource().sendSystemMessage(Component.literal("你并不在暗域"));
                        return 0;
                    }
                    LongArmsRoom.escape(p, e);
                    ctx.getSource().sendSystemMessage(Component.literal("你逃出了暗域"));
                    return 1;
                })));
    }
}
