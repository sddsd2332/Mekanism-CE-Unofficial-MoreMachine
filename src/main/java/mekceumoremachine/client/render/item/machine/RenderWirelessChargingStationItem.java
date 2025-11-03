package mekceumoremachine.client.render.item.machine;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.item.MekanismItemStackRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelWirelessChargingStation;
import mekceumoremachine.common.MEKCeuMoreMachine;
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
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        if (transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND || transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND) {
            GlStateManager.rotate(180, 0, 1, 0);
            GlStateManager.translate(0, 0.4F, 0);
            if (transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND) {
                GlStateManager.rotate(-45, 0, 1, 0);
            } else {
                GlStateManager.rotate(45, 0, 1, 0);
            }
            GlStateManager.rotate(50, 1, 0, 0);
            GlStateManager.scale(2.0F, 2.0F, 2.0F);
            GlStateManager.translate(0, -0.4F, 0);
        } else {
            if (transformType == ItemCameraTransforms.TransformType.GUI) {
                GlStateManager.rotate(90, 0, 1, 0);
            } else if (transformType == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND) {
                GlStateManager.rotate(180, 0, 1, 0);
            }
            GlStateManager.translate(0, 0.4F, 0);
        }
        MekanismRenderer.bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "WirelessChargingStation.png"));
        cube.renderModel(0.016F);
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
