package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import com.example.corruption.entity.CorruptedCorpseEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CorruptedCorpseRenderer extends MobRenderer<CorruptedCorpseEntity, CorruptedCorpseModel<CorruptedCorpseEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "textures/entity/corrupted_corpse.png");

    public CorruptedCorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new CorruptedCorpseModel<>(context.bakeLayer(CorruptedCorpseModelLayer.CORPSE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CorruptedCorpseEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(CorruptedCorpseEntity entity, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return RenderType.entityCutoutNoCull(getTextureLocation(entity));
    }
}
