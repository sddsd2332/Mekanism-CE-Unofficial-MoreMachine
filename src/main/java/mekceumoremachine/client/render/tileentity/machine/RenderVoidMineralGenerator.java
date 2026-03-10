package mekceumoremachine.client.render.tileentity.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelVoidMineralGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class RenderVoidMineralGenerator extends TileEntitySpecialRenderer<TileEntityVoidMineralGenerator> {
    private ModelVoidMineralGenerator model = new ModelVoidMineralGenerator();

    @Override
    public void render(TileEntityVoidMineralGenerator tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "VoidMineralGenerator_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderTier(0.0625F);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "VoidMineralGenerator.png"));
        model.render(0.0625F);
        GlStateManager.popMatrix();
        MekanismRenderer.machineRenderer().render(tileEntity, x, y, z, partialTick, destroyStage, alpha);
    }
}
