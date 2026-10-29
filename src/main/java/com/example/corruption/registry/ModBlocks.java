package com.example.corruption.registry;

import com.example.corruption.CorruptionMod;
import com.example.corruption.block.CursedTorchBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> REGISTER =
            DeferredRegister.create(Registries.BLOCK, CorruptionMod.MODID);

    public static final DeferredHolder<Block, CursedTorchBlock> CURSED_TORCH = REGISTER.register("cursed_torch",
            () -> new CursedTorchBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.5F)
                    .lightLevel(state -> state.getValue(CursedTorchBlock.LIT) ? 15 : 0)));

    private ModBlocks() {}
}
