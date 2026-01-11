package mekceumoremachine.client.render.item.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelReplicatorBase;
import mekceumoremachine.common.MEKCeuMoreMachine;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RenderReplicatorGasesItem extends MekanismItemStackRenderer {

    public static ItemLayerWrapper model;

    private static ModelReplicatorBase cube = new ModelReplicatorBase();

    @Override
    protected void renderBlockSpecific(@NotNull ItemStack itemStack, TransformType transformType) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.translate(0, -1, 0);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "replicator_gases.png"));
        cube.renderItem(0.0625F);
        GlStateManager.popMatrix();
    }

    @Override
    protected void renderItemSpecific(@NotNull ItemStack itemStack, TransformType transformType) {

    }

    @Override
    protected @NotNull TransformType getTransform(@NotNull ItemStack itemStack) {
        return model.getTransform();
    }
}
