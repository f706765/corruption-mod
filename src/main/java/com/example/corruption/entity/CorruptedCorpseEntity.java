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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 腐化尸体：玩家在腐化较高时死亡后，原来的躯体被腐化操控而留下的实体。
 * 它会主动攻击附近的玩家（命中附带流血）。玩家需要：
 *   1. 用布包裹（severe 时须先用铅块裹住）共 3 次；
 *   2. 用铲子将其埋入土中（会在地表堆起一座小坟包）；
 *   3. 埋好后玩家腐化清零、流血解除。
 *
 * 与玩家的右键交互由 CorruptionPlayerEvents（PlayerInteractEvent.EntityInteract）统一处理，
 * 这里只暴露状态访问器与埋葬逻辑。
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
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    public void setSevere(boolean severe) {
        this.severe = severe;
    }

    public int getWrapLevel() { return wrapLevel; }
    public boolean isLeadWrapped() { return leadWrapped; }
    public boolean isSevere() { return severe; }

    /** 用布包裹 +1（由事件在判定通过后调用）。 */
    public void addWrap() { if (wrapLevel < 3) wrapLevel++; }

    /** 用铅块包裹（由事件在判定通过后调用）。 */
    public void setLeadWrapped() { this.leadWrapped = true; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 埋葬：在地表堆起一座 3×3 小坟包，并镇压玩家腐化。 */
    public void bury(ServerLevel level, Player player) {
        BlockPos center = this.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int y = level.getMaxBuildHeight();
                while (y > level.getMinBuildHeight() + 1
                        && level.getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
                    y--;
                }
                BlockPos surface = new BlockPos(x, y, z);
                BlockPos above = surface.above();
                if (level.getBlockState(above).isAir()) {
                    level.setBlock(above, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }

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
