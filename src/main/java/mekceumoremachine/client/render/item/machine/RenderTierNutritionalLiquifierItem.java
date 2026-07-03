package mekceumoremachine.client.render.item.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelTierNutritionalLiquifier;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.block.states.BlockStateTierNutritionalLiquifier.MachineType;
import mekceumoremachine.common.tier.MachineTier;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RenderTierNutritionalLiquifierItem extends MekanismItemStackRenderer {

    public static ItemLayerWrapper model;

    private static ModelTierNutritionalLiquifier cube = new ModelTierNutritionalLiquifier();

    @Override
    protected void renderBlockSpecific(@NotNull ItemStack itemStack, TransformType transformType) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.translate(0, -1.0F, 0);
        MachineType type = MachineType.get(itemStack);
        if (type == null) {
            MekanismRenderer.bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.RENDER, "NutritionalLiquifier.png"));
            cube.renderTier(0.0625F);
            cube.render(0.0625F, false);
            cube.renderBlade(0.0625F);
            cube.renderGlass(0.0625F, false);
        } else {
            MachineTier tier = type.tier;
            MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "NutritionalLiquifier_" + tier.getBaseTier().getSimpleName() + ".png"));
            cube.renderTier(0.0625F);
            MekanismRenderer.bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.RENDER, "NutritionalLiquifier.png"));
            cube.render(0.0625F, false);
            cube.renderBlade(0.0625F);
            cube.renderGlass(0.0625F, false);
        }


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
