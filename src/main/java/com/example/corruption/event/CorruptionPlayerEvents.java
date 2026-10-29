package com.example.corruption.event;

import com.example.corruption.registry.ModEffects;
import com.example.corruption.entity.CorruptedCorpseEntity;
import com.example.corruption.system.CorruptionSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 玩家侧服务端逻辑：
 * - 腐化傀儡：腐化满 100 锁死移动，直到死亡
 * - 复活：恢复默认行走速度（避免从傀儡状态直接重生仍被锁）
 * - 被腐化尸体命中 → 附加流血
 * - 手持打火石 + 携带火药 + 处于流血 → 自焚简易包扎（解除流血）
 */
public final class CorruptionPlayerEvents {
    private static final int PUPPET_THRESHOLD = 100;

    private CorruptionPlayerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.player;
        if (p.isCreative() || p.isSpectator() || !(p instanceof ServerPlayer sp)) return;

        int c = CorruptionSavedData.get(sp.getServer().overworld())
                .get(sp.getUUID(), sp.serverLevel().getGameTime());

        if (c >= PUPPET_THRESHOLD) {
            // 腐化傀儡：锁死位移，但仍可转头张望
            sp.setSpeed(0.0F);
            var d = sp.getDeltaMovement();
            sp.setDeltaMovement(0.0D, d.y, 0.0D);
            sp.setJumping(false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.setSpeed(0.1F); // 恢复默认行走速度
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp
                && event.getSource().getEntity() instanceof CorruptedCorpseEntity) {
            sp.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 600, 0)); // 30 秒
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player p = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FlintAndSteelItem)) return;
        if (!p.hasEffect(ModEffects.BLEEDING)) return;
        if (!(p instanceof ServerPlayer sp)) return;
        if (!hasGunpowder(sp)) return;

        consumeGunpowder(sp);
        sp.removeEffect(ModEffects.BLEEDING);
        sp.setSecondsOnFire(1);
        sp.sendSystemMessage(Component.literal("你用火药点燃自己做了简易包扎，火辣辣的疼，但血止住了。").withStyle(ChatFormatting.GOLD));
        event.setCanceled(true);
    }

    private static boolean hasGunpowder(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(Items.GUNPOWDER)) return true;
        }
        return false;
    }

    private static void consumeGunpowder(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(Items.GUNPOWDER)) {
                s.shrink(1);
                return;
            }
        }
    }
}
