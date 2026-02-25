package mekceumoremachine.client.render.item.generator;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.base.ITierItem;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.client.model.ModelAdvancedSolarGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderTierAdvancedSolarGeneratorItem extends MekanismItemStackRenderer {

    public static ItemLayerWrapper model;
    private static ModelAdvancedSolarGenerator advancedSolarGenerator = new ModelAdvancedSolarGenerator();


    @Override
    protected void renderBlockSpecific(@NotNull ItemStack stack, TransformType transformType) {
        MachineTier tier = MachineTier.values()[((ITierItem) stack.getItem()).getBaseTier(stack).ordinal()];
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0.2F, 0);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "AdvancedSolarGenerator_" +  tier.getBaseTier().getSimpleName() + ".png"));
        advancedSolarGenerator.render(0.022F);
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
