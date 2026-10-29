package com.example.corruption;

import com.example.corruption.client.WatcherModel;
import com.example.corruption.client.WatcherModelLayer;
import com.example.corruption.client.WatcherRenderer;
import com.example.corruption.event.CorruptionEvents;
import com.example.corruption.registry.ModAttributes;
import com.example.corruption.registry.ModEntities;
import com.example.corruption.registry.ModItems;
import com.example.corruption.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(CorruptionMod.MODID)
public class CorruptionMod {
    public static final String MODID = "corruption";

    public CorruptionMod(IEventBus modBus, ModContainer container) {
        ModEntities.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);

        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerLayerDefinitions);

        NeoForge.EVENT_BUS.register(CorruptionEvents.class);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WATCHER.get(), ModAttributes.watcherAttributes().build());
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
    }

    private void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WatcherModelLayer.WATCHER, WatcherModel::createBodyLayer);
    }
}
