package mekceumoremachine.client.render.item.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.base.ITierItem;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelWirelessChargingStation;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tier.MachineTier;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderWirelessChargingStationItem extends MekanismItemStackRenderer {

    public static ItemLayerWrapper model;

    private static ModelWirelessChargingStation cube = new ModelWirelessChargingStation();

    @Override
    protected void renderBlockSpecific(@Nonnull ItemStack stack, ItemCameraTransforms.TransformType transformType) {
        MachineTier tier = MachineTier.values()[((ITierItem) stack.getItem()).getBaseTier(stack).ordinal()];
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.translate(0, -0.125F, 0);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER,"Wireless_Charging_" + tier.getBaseTier().getSimpleName() + ".png"));
        cube.renderModelTier(0.034F);
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "Wireless_Charging.png"));
        cube.renderModel(0.034F);
        GlStateManager.popMatrix();
    }


    @Override
    protected void renderItemSpecific(@Nonnull ItemStack stack, ItemCameraTransforms.TransformType transformType) {
    }


    @Nonnull
    @Override
    protected ItemCameraTransforms.TransformType getTransform(@Nonnull ItemStack stack) {
        return model.getTransform();
    }
}
