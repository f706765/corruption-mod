package com.example.corruption.effect;

import com.example.corruption.CorruptionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 流血（有血）效果：
 * - 行走变慢（自动附加 MOVEMENT_SPEED 乘数修正）
 * - 每 5 秒失去半颗心（由 CorruptionEvents 服务端每秒统一结算，避免覆写 tick 方法的版本差异）
 * - 屏幕变暗 / 可视距离缩短（由客户端 CorruptionClientEffects 渲染）
 * 解除方式：绷带包扎，或用火药+打火石自焚简易包扎。
 */
public class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8a0303);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "bleeding_slow"),
                -0.35D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
