package mekceumoremachine.client.render.item.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.base.ITierItem;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelVoidMineralGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderVoidMineralGeneratorItem extends MekanismItemStackRenderer {
    public static ItemLayerWrapper model;
    private ModelVoidMineralGenerator cube = new ModelVoidMineralGenerator();

    @Override
    protected void renderBlockSpecific(@NotNull ItemStack stack, ItemCameraTransforms.TransformType transformType) {
        MachineTier tier = MachineTier.values()[((ITierItem) stack.getItem()).getBaseTier(stack).ordinal()];
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.scale(0.6F, 0.6F, 0.6F);
        GlStateManager.translate(0, -0.55F, 0);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "VoidMineralGenerator_" + tier.getBaseTier().getSimpleName() + ".png"));
        cube.renderTier(0.0625F);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "VoidMineralGenerator.png"));
        cube.render(0.0625F);
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
