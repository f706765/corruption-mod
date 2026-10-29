package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import com.example.corruption.registry.ModEffects;
import com.example.corruption.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 客户端特效（全部 Client Side，绝不触碰服务端逻辑）：
 * - 腐化 ≥40：身体周围浮现灰/黑腐化迹象粒子，越腐化越密
 * - 腐化 ≥50：玩家视野四周（半径 3–5 格）环形生成黑雾粒子（暗角）
 * - 腐化 ≥75：循环播放低频心跳，频率随腐化升高加快
 * - 腐化 ≥90：镜头轻微倾斜/摇晃
 * - 流血（有血）效果：屏幕暗红血雾 + 可视距离缩短到 ~4 格 + 行走变慢（由效果属性自动处理）
 *
 * 不使用 @EventBusSubscriber 注解，改为在主类 Dist.CLIENT 下手动注册，
 * 以兼容 NeoForge 21.1 的事件总线分发。
 */
public final class CorruptionClientEffects {
    private static int heartbeatCooldown = 0;
    private static long cameraFrame = 0;
    private static boolean wasBleeding = false;
    private static int savedRenderDist = 12;

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

        // ≥40：腐化迹象
        if (corruption >= 40) {
            spawnCorruptionSigns(level, player, corruption);
        }

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

        // 流血：可视距离缩短
        boolean bleeding = player.hasEffect(ModEffects.BLEEDING);
        if (bleeding && !wasBleeding) {
            savedRenderDist = mc.options.renderDistance().get();
            mc.options.renderDistance().set(4);
            wasBleeding = true;
        } else if (!bleeding && wasBleeding) {
            mc.options.renderDistance().set(savedRenderDist);
            wasBleeding = false;
        }
    }

    /** 在玩家身体周围（脚到头）浮现灰/黑腐化粒子。 */
    private static void spawnCorruptionSigns(Level level, Player player, int corruption) {
        float t = Mth.clamp((corruption - 40) / 60.0F, 0.0F, 1.0F);
        int count = 1 + Math.round(t * 3);
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (Math.random() - 0.5) * 1.0D;
            double y = player.getY() + Math.random() * 1.8D;
            double z = player.getZ() + (Math.random() - 0.5) * 1.0D;
            if (corruption >= 70) {
                level.addParticle(ParticleTypes.SQUID_INK, x, y, z, 0.0, 0.0, 0.0);
            } else {
                level.addParticle(ParticleTypes.ASH, x, y, z, 0.0, 0.0, 0.0);
            }
        }
    }

    /** 在玩家四周（跟随视角）半径 3–5 格处生成环形黑雾粒子，不糊脸。 */
    private static void spawnVignetteParticles(Level level, Player player, int corruption) {
        float t = (corruption - 50) / 50.0F;                       // 0..1
        int count = 1 + Math.round(t * 3);                         // 1..4 / tick
        double viewYaw = Math.toRadians(player.getYRot());

        for (int i = 0; i < count; i++) {
            double ang = viewYaw + (Math.random() - 0.5) * Math.PI * 2.0;
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

    /** 流血时的暗红血雾 + 四周压暗。 */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.hasEffect(ModEffects.BLEEDING)) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        g.fill(0, 0, w, h, 0x55130000);                 // 整屏淡红血雾
        int edge = Math.min(w, h) / 4;
        g.fill(0, 0, w, edge, 0x88100000);              // 上
        g.fill(0, h - edge, w, h, 0x88100000);          // 下
        g.fill(0, 0, edge, h, 0x88100000);              // 左
        g.fill(w - edge, 0, w, h, 0x88100000);          // 右
    }
}
