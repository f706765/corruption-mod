package com.example.corruption.registry;

import com.example.corruption.CorruptionMod;
import com.example.corruption.effect.BleedingEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER =
            DeferredRegister.create(Registries.MOB_EFFECT, CorruptionMod.MODID);

    public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING =
            REGISTER.register("bleeding", BleedingEffect::new);

    private ModEffects() {}
}
