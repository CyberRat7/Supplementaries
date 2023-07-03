package net.mehvahdjukaar.supplementaries.client.renderers.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.supplementaries.client.ClientSpecialModelsManager;
import net.mehvahdjukaar.supplementaries.client.ModMaterials;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.mehvahdjukaar.moonlight.api.client.util.RotHlpr;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluid;
import net.mehvahdjukaar.supplementaries.client.renderers.VertexUtils;
import net.mehvahdjukaar.supplementaries.common.block.tiles.JarBlockTile;
import net.mehvahdjukaar.supplementaries.reg.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class JarBlockTileRenderer extends CageBlockTileRenderer<JarBlockTile> {
    private final ItemRenderer itemRenderer;
    private final Minecraft minecraft = Minecraft.getInstance();

    public JarBlockTileRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        itemRenderer = minecraft.getItemRenderer();
    }


    public static void renderFluid(float percentageFill, int color, int luminosity, ResourceLocation texture, PoseStack poseStack, MultiBufferSource bufferIn, int light, int combinedOverlayIn) {
        poseStack.pushPose();
        if (luminosity != 0) light = light & 15728640 | luminosity << 4;
        VertexConsumer builder = ModMaterials.get(texture).buffer(bufferIn, RenderType::entityTranslucentCull);
        Vector3f dimensions = ClientSpecialModelsManager.getJarLiquidDimensions();
        poseStack.translate(0.5, dimensions.z(), 0.5);

        VertexUtils.addCube(builder, poseStack,
                dimensions.x(),
                percentageFill * dimensions.y(),
                 light, color);

        poseStack.popPose();
    }

    @Override
    public void render(JarBlockTile tile, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        long r = tile.getBlockPos().asLong();
        Random rand = new Random(r);
        //render cookies
        if (!tile.isEmpty()) {
            ItemStack stack = tile.getDisplayedItem();
            int height = tile.getDisplayedItem().getCount();
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.5, 0.5, 0.5);
            matrixStackIn.mulPose(RotHlpr.XN90);
            matrixStackIn.translate(0, 0, -0.5);
            float scale = 8f / 14f;
            matrixStackIn.scale(scale, scale, scale);
            for (float i = 0; i < height; i++) {
                matrixStackIn.mulPose(Axis.ZP.rotationDegrees(rand.nextInt(16) * 22.5f));
                // matrixStackIn.translate(0, 0, 0.0625);
                matrixStackIn.translate(0, 0, 1 / (16f * scale));
                BakedModel model = itemRenderer.getModel(stack, tile.getLevel(), null, 0);
                itemRenderer.render(stack, ItemDisplayContext.FIXED, true, matrixStackIn, bufferIn, combinedLightIn,
                        combinedOverlayIn, model);
            }
            matrixStackIn.popPose();
        }
        //render fish
        var data = tile.mobContainer.getData();
        if (data != null) {
            if (data.is2DFish()) {
                matrixStackIn.pushPose();

                long time = System.currentTimeMillis() + r;
                float angle = (time % (360 * 80)) / 80f;
                float angle2 = (time % (360 * 3)) / 3f;
                float angle3 = (time % (360 * 350)) / 350f;
                float wo = 0.015f * Mth.sin((float) (2 * Math.PI * angle2 / 360));
                float ho = 0.1f * Mth.sin((float) (2 * Math.PI * angle3 / 360));
                VertexConsumer builder = bufferIn.getBuffer(RenderType.cutout());
                matrixStackIn.translate(0.5, 0.5, 0.5);
                Quaternionf rotation = Axis.YP.rotationDegrees(-angle);
                matrixStackIn.mulPose(rotation);
                matrixStackIn.scale(0.625f, 0.625f, 0.625f);
                Vector3f dimensions = ClientSpecialModelsManager.getJarLiquidDimensions();

                matrixStackIn.translate(0, -0.2, -0.335 * (dimensions.x() / 0.5f));
                int fishType = data.getFishTexture();

                //overlay
                VertexUtils.renderFish(builder, matrixStackIn, wo, ho, fishType, combinedLightIn);
                matrixStackIn.popPose();

            } else {
                super.render(tile, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
            }
            var fluid = tile.mobContainer.shouldRenderWithFluid();
            if (fluid != null && fluid.isPresent()) {
                if (fluid.get() == BuiltInSoftFluids.WATER.get()) {
                    //sand
                    matrixStackIn.pushPose();
                    Vector3f dimensions = ClientSpecialModelsManager.getJarLiquidDimensions();

                    matrixStackIn.translate(0.5, 0.0015 + dimensions.z(), 0.5);

                    VertexConsumer builder = ModMaterials.SAND_MATERIAL.buffer(bufferIn, RenderType::entityCutout);
                    VertexUtils.addCube(builder, matrixStackIn, 0.99f * dimensions.x(), dimensions.y() / 12,
                             combinedLightIn, -1);
                    matrixStackIn.popPose();
                }
                matrixStackIn.pushPose();
                SoftFluid s = fluid.get();
                renderFluid(9 / 12f, s.getTintColor(), 0, s.getStillTexture(),
                        matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
                matrixStackIn.popPose();
            }
        }
        //render fluid
        if (!tile.fluidHolder.isEmpty()) {
            renderFluid(tile.fluidHolder.getHeight(1), tile.fluidHolder.getTintColor(tile.getLevel(), tile.getBlockPos()),
                    tile.fluidHolder.getFluid().getLuminosity(), tile.fluidHolder.getFluid().getStillTexture(),
                    matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
        }
    }
}

