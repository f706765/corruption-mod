package com.example.corruption.item;

import com.example.corruption.network.CorruptionSyncPacket;
import com.example.corruption.registry.ModSounds;
import com.example.corruption.system.CorruptionSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 钳子：把身上的腐化硬掰下来。
 * 右键使用：若腐化值 > 0，则腐化 -20（最低 0），代价是撕裂伤口痛失 3 颗心。
 */
public class PliersItem extends Item {
    private static final int REDUCE = 20;
    private static final float SELF_DAMAGE = 6.0F; // 3 颗心

    public PliersItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        MinecraftServer server = level.getServer();
        if (server == null) return InteractionResultHolder.pass(stack);
        ServerLevel overworld = server.overworld();
        int c = CorruptionSavedData.get(overworld).get(player.getUUID(), overworld.getGameTime());

        if (c <= 0) {
            player.sendSystemMessage(Component.literal("你身上没有可被钳子掰下的腐化。").withStyle(ChatFormatting.GRAY));
            return InteractionResultHolder.pass(stack);
        }

        int newC = Math.max(0, c - REDUCE);
        CorruptionSavedData.get(overworld).set(player.getUUID(), newC, overworld.getGameTime());
        PacketDistributor.sendToPlayer((ServerPlayer) player, new CorruptionSyncPacket(newC));
        player.hurt(player.damageSources().generic(), SELF_DAMAGE);
        player.sendSystemMessage(Component.literal("你用钳子掰下了腐化（-" + REDUCE + "），但撕裂的伤口让你痛失 3 颗心。").withStyle(ChatFormatting.RED));
        return InteractionResultHolder.success(stack);
    }
}
