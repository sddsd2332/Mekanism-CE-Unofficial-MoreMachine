package mekceumoremachine.client.render.item.generator;


import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.base.ITierItem;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.generators.common.MekanismGenerators;
import mekceumoremachine.client.model.generator.ModelTierWindGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderBigWindGeneratorItem extends MekanismItemStackRenderer {

    public static ItemLayerWrapper model;
    private static ModelTierWindGenerator windGenerator = new ModelTierWindGenerator();
    private static int angle = 0;
    private static float lastTicksUpdated = 0;

    @Override
    protected void renderBlockSpecific(@Nonnull ItemStack stack, ItemCameraTransforms.TransformType transformType) {
        MachineTier tier = MachineTier.values()[((ITierItem) stack.getItem()).getBaseTier(stack).ordinal()];
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        if (transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND || transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND) {
            GlStateManager.rotate(180, 0, 1, 0);
            GlStateManager.translate(0, 0.4F, 0);
            if (transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND) {
                GlStateManager.rotate(-45, 0, 1, 0);
            } else {
                GlStateManager.rotate(45, 0, 1, 0);
            }
            GlStateManager.rotate(50, 1, 0, 0);
            GlStateManager.scale(2.0F, 2.0F, 2.0F);
            GlStateManager.translate(0, -0.4F, 0);
        } else {
            if (transformType == ItemCameraTransforms.TransformType.GUI) {
                GlStateManager.rotate(90, 0, 1, 0);
            } else if (transformType == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND) {
                GlStateManager.rotate(180, 0, 1, 0);
            }
            GlStateManager.translate(0, 0.4F, 0);
        }


        //TODO: Only update angle if the player is not in a blacklisted dimension, one that has no "wind".
        //The best way to do this would be to add an event listener for dimension change.
        //The event is server side only so we would need to send a packet to clients to tell them if they are
        //in a blacklisted dimension or not.
        if (MekanismConfig.current().client.windGeneratorItem.val()) {
            if (lastTicksUpdated != Minecraft.getMinecraft().getRenderPartialTicks()) {
                angle = (angle + 2) % 360;
                lastTicksUpdated = Minecraft.getMinecraft().getRenderPartialTicks();
            }
        } else {
            angle = 0;
        }
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, ResourceType.RENDER, "WindGenerator_Big.png"));
        windGenerator.renderHead(0.016F);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MekanismGenerators.MODID, ResourceType.RENDER, "WindGenerator.png"));
        windGenerator.renderItem(0.016F, angle);
        GlStateManager.popMatrix();
    }

    @Override
    protected void renderItemSpecific(@Nonnull ItemStack stack, ItemCameraTransforms.TransformType transformType) {
    }


    @Nonnull
    @Override
    protected ItemCameraTransforms.TransformType getTransform(@Nonnull ItemStack stack) {
        return model.getTransform();
    }
}
