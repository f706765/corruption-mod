package com.example.corruption.registry;

import com.example.corruption.CorruptionMod;
import com.example.corruption.entity.WatcherEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ForgeSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(CorruptionMod.MODID);

    public static final Item WATCHER_SPAWN_EGG = REGISTER.registerItem("watcher_spawn_egg",
            props -> new ForgeSpawnEggItem(ModEntities.WATCHER, 0x0d0d14, 0x6e1010, props),
            new Item.Properties());

    private ModItems() {}
}
