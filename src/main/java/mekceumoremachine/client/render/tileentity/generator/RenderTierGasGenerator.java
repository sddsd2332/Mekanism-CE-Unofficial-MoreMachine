package mekceumoremachine.client.render.tileentity.generator;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.generators.common.MekanismGenerators;
import mekceumoremachine.client.model.generator.ModelTierGasGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderTierGasGenerator extends TileEntitySpecialRenderer<TileEntityTierGasGenerator> {

    private ModelTierGasGenerator model = new ModelTierGasGenerator();

    @Override
    public void render(TileEntityTierGasGenerator tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        MekanismRenderer.rotate(tileEntity.facing, 90, 270, 180, 0);
        GlStateManager.rotate(180, 0, 1, 1);
        GlStateManager.rotate(90, -1, 0, 0);
        GlStateManager.rotate(90, 0, 1, 0);
        bindTexture(MekanismUtils.getResource(MekanismGenerators.MODID, ResourceType.RENDER, "GasGenerator.png"));
        model.render(0.0625F);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, ResourceType.RENDER, "GasGenerator_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderTier(0.0625F);
        GlStateManager.popMatrix();
    }

}
