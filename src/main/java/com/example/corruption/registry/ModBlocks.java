package com.example.corruption.registry;

import com.example.corruption.CorruptionMod;
import com.example.corruption.block.CursedTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> REGISTER =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK, CorruptionMod.MODID);

    public static final DeferredBlock<CursedTorchBlock> CURSED_TORCH = REGISTER.register("cursed_torch",
            () -> new CursedTorchBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.5F)));

    private ModBlocks() {}
}
