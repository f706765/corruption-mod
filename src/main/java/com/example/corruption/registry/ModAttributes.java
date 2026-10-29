package com.example.corruption.registry;

import com.example.corruption.entity.WatcherEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ModAttributes {
    public static AttributeSupplier.Builder watcherAttributes() {
        return WatcherEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.19D)   // 低速：制造压迫而非追逐
                .add(Attributes.FOLLOW_RANGE, 64.0D)     // 大感知：它一直在“注意”你
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    private ModAttributes() {}
}
