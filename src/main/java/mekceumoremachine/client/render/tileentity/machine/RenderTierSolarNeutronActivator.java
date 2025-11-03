package mekceumoremachine.client.render.tileentity.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelTierSolarNeutronActivator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderTierSolarNeutronActivator extends TileEntitySpecialRenderer<TileEntityTierSolarNeutronActivator> {

    private ModelTierSolarNeutronActivator model = new ModelTierSolarNeutronActivator();

    @Override
    public void render(TileEntityTierSolarNeutronActivator tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "SolarNeutronActivator_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderTank(0.0625F);
        bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.RENDER, "SolarNeutronActivator.png"));
        model.render(0.0625F, tileEntity.getActive());
        GlStateManager.popMatrix();
        MekanismRenderer.machineRenderer().render(tileEntity, x, y, z, partialTick, destroyStage, alpha);
    }
}
