package mekceumoremachine.client.render.tileentity.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelReplicatorBase;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class RenderReplicatorItemStack extends TileEntitySpecialRenderer<TileEntityReplicatorItemStack> {

    private final ModelReplicatorBase model = new ModelReplicatorBase();


    @Override
    public void render(TileEntityReplicatorItemStack tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "replicator_itemstack.png"));
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        model.renderModel(0.0625F, true);
        GlStateManager.popMatrix();
        MekanismRenderer.machineRenderer().render(tileEntity, x, y, z, partialTick, destroyStage, alpha);
    }

    public ModelReplicatorBase getModel() {
        return model;
    }

}
