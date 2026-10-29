package com.example.corruption.registry;

import com.example.corruption.CorruptionMod;
import com.example.corruption.entity.CorruptedCorpseEntity;
import com.example.corruption.entity.LongArmsEntity;
import com.example.corruption.entity.WatcherEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(Registries.ENTITY_TYPE, CorruptionMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WatcherEntity>> WATCHER =
            REGISTER.register("watcher", () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F)
                    .clientTrackingRange(10)
                    .build("watcher"));

    public static final DeferredHolder<EntityType<?>, EntityType<LongArmsEntity>> LONG_ARMS =
            REGISTER.register("long_arms", () -> EntityType.Builder.of(LongArmsEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 3.0F)
                    .clientTrackingRange(16)
                    .build("long_arms"));

    public static final DeferredHolder<EntityType<?>, EntityType<CorruptedCorpseEntity>> CORRUPTED_CORPSE =
            REGISTER.register("corrupted_corpse", () -> EntityType.Builder.of(CorruptedCorpseEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.0F)
                    .clientTrackingRange(16)
                    .build("corrupted_corpse"));

    private ModEntities() {}
}
