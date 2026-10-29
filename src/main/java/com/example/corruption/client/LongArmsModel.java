package com.example.corruption.client;

import com.example.corruption.entity.LongArmsEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 长臂模型：全身漆黑、长臂长腿的瘦高人形。
 * 贴图纯黑，长臂垂到腿部、长腿接近两格，呈现“不自然伸长”的恐怖感。
 */
public class LongArmsModel<T extends LongArmsEntity> extends HierarchicalModel<T> {
    private final ModelPart root;

    public LongArmsModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 长臂：从肩部下垂约 22 单位（约 1.4 格），比躯干更长
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(32, 16).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 22.0F, 4.0F),
                PartPose.offset(-5.0F, 1.0F, 0.0F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(32, 16).mirror().addBox(-2.0F, -2.0F, -2.0F, 4.0F, 22.0F, 4.0F),
                PartPose.offset(5.0F, 1.0F, 0.0F));

        // 长腿：约 26 单位（约 1.6 格）
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 26.0F, 4.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 26.0F, 4.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 完全静止：不摆动、不呼吸——“不自然”本身就是恐怖
        this.root.getAllParts().forEach(ModelPart::resetPose);
    }
}
