package com.example.corruption.client;

import com.example.corruption.CorruptionMod;
import com.example.corruption.entity.WatcherEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WatcherRenderer extends MobRenderer<WatcherEntity, WatcherModel<WatcherEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "textures/entity/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new WatcherModel<>(context.bakeLayer(WatcherModelLayer.WATCHER)), 0.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(WatcherEntity entity, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        // cutout：贴图透明部分完全透出，形成“轮廓残缺”的效果
        return RenderType.entityCutoutNoCull(getTextureLocation(entity));
    }
}
