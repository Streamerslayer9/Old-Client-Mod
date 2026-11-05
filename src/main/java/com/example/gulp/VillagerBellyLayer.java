package com.example.gulp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * The belly for real Minecraft Comes Alive villagers (BellyLayer only covers players). How full the belly is comes from
 * the same BellyPacket the players use, keyed by the villager's UUID (see VillagerHolding.syncBelly).
 */
public class VillagerBellyLayer<T extends LivingEntity, M extends PlayerModel<T>> extends RenderLayer<T, M> {
    private static final float MAX_EXTRA_DEPTH = 1.6f;
    private static final float MAX_EXTRA_WIDTH = 0.18f;

    private final ModelPart bellyFull, jacketFull, bellyLow, jacketLow;
    private final ModelPart bellyFullClothing, jacketFullClothing, bellyLowClothing, jacketLowClothing;
    private final RenderLayerParent<T, M> parent;
    private List<Object> mcaLayers;

    public VillagerBellyLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(ClientModEvents.BELLY_LAYER);
        this.bellyFull = root.getChild("belly_full");
        this.jacketFull = root.getChild("jacket_full");
        this.bellyLow = root.getChild("belly_low");
        this.jacketLow = root.getChild("jacket_low");
        this.bellyFullClothing = root.getChild("belly_full_clothing");
        this.jacketFullClothing = root.getChild("jacket_full_clothing");
        this.bellyLowClothing = root.getChild("belly_low_clothing");
        this.jacketLowClothing = root.getChild("jacket_low_clothing");
        this.parent = parent;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        float shown = ClientState.smoothedBelly(entity.getUUID());
        if (shown < 0.02f) return;
        if (GulpConfig.bellyStyle == GulpConfig.BellyStyle.OFF) return;

        boolean classic = GulpConfig.bellyStyle == GulpConfig.BellyStyle.CLASSIC;
        ModelPart belly = classic ? bellyFull : bellyLow;
        ModelPart jacket = classic ? jacketFull : jacketLow;

        float bulge = (float) Math.sqrt(shown); // sqrt so even a small meal is visible
        float depthScale = 1f + bulge * MAX_EXTRA_DEPTH;
        float widthScale = 1f + bulge * MAX_EXTRA_WIDTH;

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0f, 0f, 2f / 16f);
        poseStack.scale(widthScale, 1f, depthScale);
        poseStack.translate(0f, 0f, -2f / 16f);

        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0f);

        if (mcaLayers == null) mcaLayers = McaSkins.findLayers(parent);
        List<McaSkins.Pass> mca = McaSkins.passes(entity, mcaLayers, partialTick);
        if (mca != null && !mca.isEmpty()) {
            for (McaSkins.Pass pass : mca) {
                VertexConsumer mcaBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(pass.texture()));
                ModelPart body = pass.clothing() ? (classic ? bellyFullClothing : bellyLowClothing) : belly;
                ModelPart wear = pass.clothing() ? (classic ? jacketFullClothing : jacketLowClothing) : jacket;
                body.render(poseStack, mcaBuffer, packedLight, overlay, pass.r(), pass.g(), pass.b(), 1f);
                wear.render(poseStack, mcaBuffer, packedLight, overlay, pass.r(), pass.g(), pass.b(), 1f);
            }
        } else {
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(parent.getTextureLocation(entity)));
            belly.render(poseStack, vc, packedLight, overlay);
            jacket.render(poseStack, vc, packedLight, overlay);
        }
        poseStack.popPose();
    }
}
