package com.example.corruption;

import com.example.corruption.block.CursedTorchBlock;
import com.example.corruption.client.CorruptionClientEffects;
import com.example.corruption.client.LongArmsModel;
import com.example.corruption.client.LongArmsModelLayer;
import com.example.corruption.client.LongArmsRenderer;
import com.example.corruption.client.WatcherModel;
import com.example.corruption.client.WatcherModelLayer;
import com.example.corruption.client.WatcherRenderer;
import com.example.corruption.event.CorruptionEvents;
import com.example.corruption.event.LongArmsEvents;
import com.example.corruption.network.CorruptionSyncPacket;
import com.example.corruption.registry.ModAttributes;
import com.example.corruption.registry.ModBlocks;
import com.example.corruption.registry.ModEntities;
import com.example.corruption.registry.ModItems;
import com.example.corruption.registry.ModSounds;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(CorruptionMod.MODID)
public class CorruptionMod {
    public static final String MODID = "corruption";

    public CorruptionMod(IEventBus modBus, ModContainer container) {
        ModEntities.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModBlocks.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);

        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerLayerDefinitions);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.register(CorruptionEvents.class);
        NeoForge.EVENT_BUS.register(LongArmsEvents.class);

        // 客户端特效（暗角/心跳/镜头摇晃）只在客户端注册，服务端不加载该类
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CorruptionClientEffects.register();
        }
    }

    /** 把刷怪蛋 / 道具加入创造模式物品栏。 */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.WATCHER_SPAWN_EGG);
            event.accept(ModItems.LONG_ARMS_SPAWN_EGG);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.CURSED_TORCH);
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WATCHER.get(), ModAttributes.watcherAttributes().build());
        event.put(ModEntities.LONG_ARMS.get(), ModAttributes.longArmsAttributes().build());
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.LONG_ARMS.get(), LongArmsRenderer::new);
    }

    private void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WatcherModelLayer.WATCHER, WatcherModel::createBodyLayer);
        event.registerLayerDefinition(LongArmsModelLayer.LONG_ARMS, LongArmsModel::createBodyLayer);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(CorruptionMod.MODID)
                .versioned("1")
                .optional()
                .playToClient(CorruptionSyncPacket.TYPE, CorruptionSyncPacket.STREAM_CODEC, CorruptionSyncPacket::handle);
    }
}
