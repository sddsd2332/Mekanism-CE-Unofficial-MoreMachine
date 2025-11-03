package mekceumoremachine.client.render.tileentity.generator;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.MekanismGenerators;
import mekceumoremachine.client.model.generator.ModelTierWindGenerator;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderTierWindGenerator extends TileEntitySpecialRenderer<TileEntityTierWindGenerator> {

    private ModelTierWindGenerator model = new ModelTierWindGenerator();

    @Override
    public void render(TileEntityTierWindGenerator tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        double angle = tileEntity.getAngle();
        if (tileEntity.getActive()) {
            angle = (tileEntity.getAngle() + ((tileEntity.getPos().getY() + 4F) / TileEntityTierWindGenerator.SPEED_SCALED) * partialTick) % 360;
        }
        bindTexture(MekanismUtils.getResource(MekanismGenerators.MODID, MekanismUtils.ResourceType.RENDER, "WindGenerator.png"));
        model.renderBlock(0.0625F, angle);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "WindGenerator_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderHead(0.0625F);
        GlStateManager.popMatrix();
    }

}
