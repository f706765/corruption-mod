package com.example.corruption.registry;

import com.example.corruption.CorruptionMod;
import com.example.corruption.block.CursedTorchBlock;
import com.example.corruption.entity.LongArmsEntity;
import com.example.corruption.entity.WatcherEntity;
import com.example.corruption.item.BandageItem;
import com.example.corruption.item.PliersItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(CorruptionMod.MODID);

    public static final DeferredItem<Item> WATCHER_SPAWN_EGG = REGISTER.registerItem("watcher_spawn_egg",
            props -> new SpawnEggItem(ModEntities.WATCHER.get(), 0x0d0d14, 0x6e1010, props),
            new Item.Properties());

    public static final DeferredItem<Item> LONG_ARMS_SPAWN_EGG = REGISTER.registerItem("long_arms_spawn_egg",
            props -> new SpawnEggItem(ModEntities.LONG_ARMS.get(), 0x05050a, 0x1a1a24, props),
            new Item.Properties());

    public static final DeferredItem<Item> CURSED_TORCH = REGISTER.registerItem("cursed_torch",
            props -> new BlockItem(ModBlocks.CURSED_TORCH.get(), props),
            new Item.Properties());

    public static final DeferredItem<Item> PLIERS = REGISTER.registerItem("pliers",
            PliersItem::new, new Item.Properties());

    public static final DeferredItem<Item> CLOTH = REGISTER.registerItem("cloth",
            Item::new, new Item.Properties());

    public static final DeferredItem<Item> BANDAGE = REGISTER.registerItem("bandage",
            BandageItem::new, new Item.Properties());

    public static final DeferredItem<Item> LEAD_BLOCK = REGISTER.registerItem("lead_block",
            props -> new BlockItem(ModBlocks.LEAD_BLOCK.get(), props),
            new Item.Properties());

    private ModItems() {}
}
