package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class LongArmsModelLayer {
    public static final ModelLayerLocation LONG_ARMS =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "long_arms"), "main");

    private LongArmsModelLayer() {}
}
