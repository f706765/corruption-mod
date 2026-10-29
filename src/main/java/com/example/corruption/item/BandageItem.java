package com.example.corruption.item;

import com.example.corruption.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 绷带：右键自我包扎，解除流血（有血）效果，消耗 1 个。
 */
public class BandageItem extends Item {
    public BandageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        if (!player.hasEffect(ModEffects.BLEEDING)) {
            player.sendSystemMessage(Component.literal("你身上没有流血，不需要包扎。").withStyle(ChatFormatting.GRAY));
            return InteractionResultHolder.pass(stack);
        }

        player.removeEffect(ModEffects.BLEEDING);
        stack.shrink(1);
        player.sendSystemMessage(Component.literal("你用绷带缠好了伤口，流血停止了。").withStyle(ChatFormatting.GREEN));
        return InteractionResultHolder.success(stack);
    }
}
