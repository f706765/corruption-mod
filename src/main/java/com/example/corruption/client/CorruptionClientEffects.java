package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import com.example.corruption.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 客户端特效（全部 Client Side，绝不触碰服务端逻辑）：
 * - 腐化 ≥50：玩家视野四周（半径 3–5 格）环形生成黑雾粒子，腐化越高越密
 * - 腐化 ≥75：循环播放低频心跳，频率随腐化升高加快
 * - 腐化 ≥90：镜头轻微倾斜/摇晃，模拟理智边缘的眩晕
 *
 * 不使用 @EventBusSubscriber 注解，改为在主类 Dist.CLIENT 下手动注册，
 * 以兼容 NeoForge 21.1 的事件总线分发。
 */
public final class CorruptionClientEffects {
    private static int heartbeatCooldown = 0;
    private static long cameraFrame = 0;

    private CorruptionClientEffects() {}

    /** 仅客户端调用：把本类的 @SubscribeEvent 方法挂到 NeoForge 事件总线。 */
    public static void register() {
        NeoForge.EVENT_BUS.register(CorruptionClientEffects.class);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        int corruption = ClientCorruptionData.getCorruption();

        // ≥50：暗角黑雾
        if (corruption >= 50) {
            spawnVignetteParticles(level, player, corruption);
        }

        // ≥75：心跳（频率随腐化升高加快）
        if (corruption >= 75) {
            heartbeatCooldown--;
            if (heartbeatCooldown <= 0) {
                float t = (corruption - 75) / 25.0F;          // 0..1
                int interval = Math.round(Mth.lerp(t, 36, 9));  // 1.8s .. 0.45s
                heartbeatCooldown = interval;
                level.playLocalSound(player.blockPosition(), ModSounds.HEARTBEAT.value(),
                        SoundSource.AMBIENT, 0.7F, 1.0F, false);
            }
        } else {
            heartbeatCooldown = 0;
        }
    }

    /** 在玩家四周（跟随视角）半径 3–5 格处生成环形黑雾粒子，不糊脸。 */
    private static void spawnVignetteParticles(Level level, Player player, int corruption) {
        float t = (corruption - 50) / 50.0F;                       // 0..1
        int count = 1 + Math.round(t * 3);                         // 1..4 / tick
        double viewYaw = Math.toRadians(player.getYRot());

        for (int i = 0; i < count; i++) {
            double ang = viewYaw + (Math.random() - 0.5) * Math.PI * 2.0; // 全环分布
            double radius = 3.0 + Math.random() * 2.0;                       // 3–5 格
            double x = player.getX() + Math.cos(ang) * radius;
            double z = player.getZ() + Math.sin(ang) * radius;
            double y = player.getEyeY() + (Math.random() - 0.5) * 2.0;
            level.addParticle(ParticleTypes.SQUID_INK, x, y, z, 0.0, 0.01, 0.0);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        int corruption = ClientCorruptionData.getCorruption();
        if (corruption < 90) return;

        cameraFrame++;
        float t = (corruption - 90) / 10.0F;                             // 0..1
        double time = cameraFrame * 0.05D;                               // 每帧推进，平滑振荡
        float amp = Mth.lerp(t, 0.02F, 0.07F);                          // ~1°..4°

        event.setRoll((float) Math.sin(time * 0.35D) * amp);
        event.setYaw(event.getYaw() + (float) Math.cos(time * 0.27D) * amp * 0.6F);
    }
}
