package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class WatcherModelLayer {
    public static final ModelLayerLocation WATCHER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "watcher"), "main");

    private WatcherModelLayer() {}
}
