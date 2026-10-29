package com.example.corruption.event;

import com.example.corruption.entity.CorruptedCorpseEntity;
import com.example.corruption.registry.ModEffects;
import com.example.corruption.registry.ModItems;
import com.example.corruption.system.CorruptionSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
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
 * - 对腐化尸体右键：布包裹（severe 须先铅块）→ 铲子埋葬
 */
public final class CorruptionPlayerEvents {
    private static final int PUPPET_THRESHOLD = 100;
    private static final int WRAP_REQUIRED = 3;

    private CorruptionPlayerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.isCreative() || p.isSpectator() || !p.isAlive()) return;
        if (!(p instanceof net.minecraft.server.level.ServerPlayer sp)) return;

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
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.setSpeed(0.1F); // 恢复默认行走速度
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp
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
        if (!(p instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        if (!hasGunpowder(sp)) return;

        consumeGunpowder(sp);
        sp.removeEffect(ModEffects.BLEEDING);
        sp.setRemainingFireTicks(20); // 自焚简易包扎：点燃自己，火辣辣但止住了血
        sp.sendSystemMessage(Component.literal("你用火药点燃自己做了简易包扎，火辣辣的疼，但血止住了。").withStyle(ChatFormatting.GOLD));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onCorpseInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof CorruptedCorpseEntity corpse)) return;
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        ItemStack stack = event.getItemStack();
        Item held = stack.getItem();

        // 用布包裹
        if (held == ModItems.CLOTH.get()) {
            if (corpse.getWrapLevel() >= WRAP_REQUIRED) {
                sp.sendSystemMessage(Component.literal("尸体已经裹好了布。").withStyle(ChatFormatting.GRAY));
            } else {
                corpse.addWrap();
                stack.shrink(1);
                sp.sendSystemMessage(Component.literal("你用布裹住了尸体（" + corpse.getWrapLevel() + "/" + WRAP_REQUIRED + "）。").withStyle(ChatFormatting.GOLD));
            }
            event.setCanceled(true);
            return;
        }

        // 用铅块封死（仅严重腐化需要）
        if (held == ModItems.LEAD_BLOCK.get()) {
            if (!corpse.isSevere()) {
                sp.sendSystemMessage(Component.literal("这具尸体腐化不重，不需要铅块。").withStyle(ChatFormatting.GRAY));
            } else if (corpse.isLeadWrapped()) {
                sp.sendSystemMessage(Component.literal("尸体已经用铅块裹过了。").withStyle(ChatFormatting.GRAY));
            } else {
                corpse.setLeadWrapped();
                stack.shrink(1);
                sp.sendSystemMessage(Component.literal("你用铅块封死了尸体，腐化被镇住了。").withStyle(ChatFormatting.GOLD));
            }
            event.setCanceled(true);
            return;
        }

        // 用铲子埋葬
        if (held instanceof ShovelItem) {
            boolean needLead = corpse.isSevere();
            boolean wrappedEnough = corpse.getWrapLevel() >= WRAP_REQUIRED;
            boolean leadOk = !needLead || corpse.isLeadWrapped();
            if (!wrappedEnough || !leadOk) {
                sp.sendSystemMessage(Component.literal("尸体还没裹好，不能直接埋。").withStyle(ChatFormatting.RED));
            } else {
                corpse.bury(sp.serverLevel(), sp);
            }
            event.setCanceled(true);
        }
    }

    private static boolean hasGunpowder(net.minecraft.server.level.ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(Items.GUNPOWDER)) return true;
        }
        return false;
    }

    private static void consumeGunpowder(net.minecraft.server.level.ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(Items.GUNPOWDER)) {
                s.shrink(1);
                return;
            }
        }
    }
}
