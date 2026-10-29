package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import com.example.corruption.entity.LongArmsEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class LongArmsRenderer extends MobRenderer<LongArmsEntity, LongArmsModel<LongArmsEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "textures/entity/longarms.png");

    public LongArmsRenderer(EntityRendererProvider.Context context) {
        super(context, new LongArmsModel<>(context.bakeLayer(LongArmsModelLayer.LONG_ARMS)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LongArmsEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(LongArmsEntity entity, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        // cutout：贴图（纯黑）不透明渲染，呈现漆黑实体
        return RenderType.entityCutoutNoCull(getTextureLocation(entity));
    }
}
