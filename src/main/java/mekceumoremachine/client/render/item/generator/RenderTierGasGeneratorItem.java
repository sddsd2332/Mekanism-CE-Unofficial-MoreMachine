package mekceumoremachine.client.render.item.generator;


import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.base.ITierItem;
import mekanism.generators.common.MekanismGenerators;
import mekceumoremachine.client.model.generator.ModelTierGasGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderTierGasGeneratorItem extends MekanismItemStackRenderer {

    public static ItemLayerWrapper model;
    private static ModelTierGasGenerator cube = new ModelTierGasGenerator();


    @Override
    protected void renderBlockSpecific(@Nonnull ItemStack stack, TransformType transformType) {
        MachineTier tier = MachineTier.values()[((ITierItem) stack.getItem()).getBaseTier(stack).ordinal()];
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 1, 1);
        GlStateManager.rotate(90, -1, 0, 0);
        GlStateManager.translate(0, -1.0F, 0);
        GlStateManager.rotate(180, 0, 1, 0);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MekanismGenerators.MODID, ResourceType.RENDER, "GasGenerator.png"));
        cube.render(0.0625F);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, ResourceType.RENDER, "GasGenerator_" + tier.getBaseTier().getSimpleName() + ".png"));
        cube.renderTier(0.0625F);
        GlStateManager.popMatrix();
    }

    @Override
    protected void renderItemSpecific(@Nonnull ItemStack stack, TransformType transformType) {
    }


    @Nonnull
    @Override
    protected TransformType getTransform(@Nonnull ItemStack stack) {
        return model.getTransform();
    }
}
