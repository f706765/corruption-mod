package com.example.corruption.registry;

import com.example.corruption.CorruptionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(Registries.SOUND_EVENT, CorruptionMod.MODID);

    // createVariableRangeEvent：音量 1.0 时默认 16 格衰减，可由 volume 控制实际听感范围
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER =
            REGISTER.register("whisper", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "whisper")));

    // heartbeat：低频心跳，客户端 ≥75 时循环播放，频率随腐化升高加快
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT =
            REGISTER.register("heartbeat", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "heartbeat")));

    private ModSounds() {}
}
