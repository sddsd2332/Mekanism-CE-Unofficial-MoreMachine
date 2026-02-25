package mekceumoremachine.client.render.tileentity.generator;

import mekanism.common.util.MekanismUtils;
import mekanism.generators.client.model.ModelSolarGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.generator.TileEntityTierSolarGenerator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class RenderTierSolarGenerator extends TileEntitySpecialRenderer<TileEntityTierSolarGenerator> {

    private ModelSolarGenerator model = new ModelSolarGenerator();

    @Override
    public void render(TileEntityTierSolarGenerator tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "SolarGenerator_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        GlStateManager.rotate(180, 0, 0, 1);
        model.render(0.0625F);
        GlStateManager.popMatrix();
    }
}
