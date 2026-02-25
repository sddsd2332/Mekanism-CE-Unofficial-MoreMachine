package mekceumoremachine.client.render.tileentity.generator;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.client.model.ModelAdvancedSolarGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.generator.TileEntityTierAdvancedSolarGenerator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class RenderTierAdvancedSolarGenerator extends TileEntitySpecialRenderer<TileEntityTierAdvancedSolarGenerator> {

    private ModelAdvancedSolarGenerator model = new ModelAdvancedSolarGenerator();

    @Override
    public void render(TileEntityTierAdvancedSolarGenerator tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "AdvancedSolarGenerator_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        model.render(0.0625F);
        GlStateManager.popMatrix();
    }
}
