package com.example.corruption.effect;

import com.example.corruption.CorruptionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 流血（有血）效果：
 * - 行走变慢（自动附加 MOVEMENT_SPEED 乘数修正）
 * - 每 5 秒（100 tick）失去半颗心
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

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return true;
        entity.hurt(entity.damageSources().generic(), 1.0F); // 半颗心
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration) {
        return duration % 100 == 0; // 每 5 秒（100 tick）触发一次
    }
}
