package com.example.corruption.entity;

import com.example.corruption.CorruptionMod;
import com.example.corruption.network.CorruptionSyncPacket;
import com.example.corruption.registry.ModEffects;
import com.example.corruption.registry.ModItems;
import com.example.corruption.system.CorruptionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 腐化尸体：玩家在腐化较高时死亡后，原来的躯体被腐化操控而留下的实体。
 * 它会主动攻击附近的玩家（命中附带流血）。玩家需要：
 *   1. 用布包裹（severe 时须先用铅块裹住）共 3 次；
 *   2. 用铲子将其埋入土中（会在地表堆起一座小坟包）；
 *   3. 埋好后玩家腐化清零、流血解除。
 */
public class CorruptedCorpseEntity extends Monster {
    private int wrapLevel = 0;          // 已用布包裹次数（0..3）
    private boolean leadWrapped = false; // 是否用铅块包裹（severe 必需）
    private boolean severe = false;      // 死亡时腐化是否过重（>=80）

    public CorruptedCorpseEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setSevere(boolean severe) {
        this.severe = severe;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollingGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            // 客户端直接放行，由服务端决定结果
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);

        // 铅块：重度腐化尸体必须先裹铅块
        if (stack.is(ModItems.LEAD_BLOCK.get())) {
            if (!severe) {
                player.sendSystemMessage(Component.literal("这具尸体还不算严重腐化，无需铅块。").withStyle(net.minecraft.ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            if (leadWrapped) {
                player.sendSystemMessage(Component.literal("已经用铅块包裹过了。").withStyle(net.minecraft.ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            leadWrapped = true;
            stack.shrink(1);
            player.sendSystemMessage(Component.literal("你用铅块死死裹住了尸体，现在可以用布包裹了。").withStyle(net.minecraft.ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }

        // 布：包裹（3 次）
        if (stack.is(ModItems.CLOTH.get())) {
            if (severe && !leadWrapped) {
                player.sendSystemMessage(Component.literal("腐化过于严重，必须先用铅块包裹才能用布。").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (wrapLevel >= 3) {
                player.sendSystemMessage(Component.literal("尸体已经裹好了，用铲子把它埋了吧。").withStyle(net.minecraft.ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            wrapLevel++;
            stack.shrink(1);
            player.sendSystemMessage(Component.literal("你用布包裹尸体（" + wrapLevel + "/3）。").withStyle(net.minecraft.ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        // 铲子：埋葬
        if (stack.getItem() instanceof ShovelItem) {
            if (severe && !leadWrapped) {
                player.sendSystemMessage(Component.literal("腐化过于严重，必须先用铅块包裹。").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (wrapLevel < 3) {
                player.sendSystemMessage(Component.literal("尸体还没裹好，先用布包裹（" + wrapLevel + "/3）。").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            bury((ServerLevel) this.level(), this.blockPosition(), player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void bury(ServerLevel level, BlockPos pos, Player player) {
        // 在尸体周围堆起一座 3×3 的小坟包（地表之上加一层泥土）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos t = pos.offset(dx, 0, dz);
                BlockPos surf = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, t);
                BlockPos above = surf.above();
                if (level.getBlockState(above).isAir()) {
                    level.setBlock(above, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }

        // 镇压：玩家腐化清零、解除流血
        var server = level.getServer();
        if (server != null) {
            ServerLevel ow = server.overworld();
            CorruptionSavedData.get(ow).set(player.getUUID(), 0, ow.getGameTime());
            PacketDistributor.sendToPlayer((ServerPlayer) player, new CorruptionSyncPacket(0));
        }
        player.removeEffect(ModEffects.BLEEDING);
        player.sendSystemMessage(Component.literal("你埋好了尸体，腐化似乎被压制下去了。").withStyle(net.minecraft.ChatFormatting.GOLD));
        this.discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
