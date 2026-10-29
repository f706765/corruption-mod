package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class CorruptedCorpseModelLayer {
    public static final ModelLayerLocation CORPSE =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "corrupted_corpse"), "main");

    private CorruptedCorpseModelLayer() {}
}
