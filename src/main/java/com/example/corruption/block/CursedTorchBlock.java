package com.example.corruption.block;

import com.example.corruption.system.LongArmsRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 诅咒火炬：暗域里需要重新点亮的三个火炬。
 * 初始为未点亮（不发光、漆黑），手持火把右键即可点亮；
 * 当三个全部点亮时，由 LongArmsRoom 判定逃脱。
 */
public class CursedTorchBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CursedTorchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public int getLightEmission(BlockState state) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    public InteractionResult useItemOn(BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(LIT)) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(Items.TORCH) && held.getCount() > 0) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(LIT, true), 3);
                    held.shrink(1);
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        LongArmsRoom.onTorchLit((net.minecraft.server.level.ServerLevel) level, sp);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }
}
