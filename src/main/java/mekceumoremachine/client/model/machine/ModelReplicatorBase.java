package mekceumoremachine.client.model.machine;

import mekanism.client.render.MekanismRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class ModelReplicatorBase extends ModelBase {

    ModelRenderer port_leds;
    ModelRenderer lights;
    ModelRenderer ports;
    ModelRenderer port_back;
    ModelRenderer glass;
    ModelRenderer bb_main;

    public ModelReplicatorBase() {
        textureWidth = 128;
        textureHeight = 128;

        port_leds = new ModelRenderer(this);
        port_leds.setRotationPoint(8.0F, 24.0F, -8.0F);
        port_leds.cubeList.add(new ModelBox(port_leds, 28, 59, -12.0F, -12.0F, 16.01F, 8, 8, 0, 0.0F, false));

        lights = new ModelRenderer(this);
        lights.setRotationPoint(8.0F, 24.0F, -8.0F);
        lights.cubeList.add(new ModelBox(lights, 22, 69, -2.0F, -10.0F, 14.0F, 1, 2, 1, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 70, 71, -4.0F, -10.0F, 14.0F, 1, 2, 1, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 22, 72, -13.0F, -10.0F, 14.0F, 1, 2, 1, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 36, 72, -15.0F, -10.0F, 14.0F, 1, 2, 1, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 40, 72, -6.0F, -5.0F, 1.0F, 1, 2, 1, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 72, 42, -11.0F, -5.0F, 1.0F, 1, 2, 1, 0.0F, false));

        ports = new ModelRenderer(this);
        ports.setRotationPoint(8.0F, 24.0F, -8.0F);


        port_back = new ModelRenderer(this);
        port_back.setRotationPoint(-5.0F, -8.0F, 13.0F);
        ports.addChild(port_back);
        port_back.cubeList.add(new ModelBox(port_back, 64, 0, -7.0F, -4.0F, 2.01F, 8, 8, 1, 0.0F, false));

        glass = new ModelRenderer(this);
        glass.setRotationPoint(1.0F, 16.0F, -7.0F);
        glass.cubeList.add(new ModelBox(glass, 0, 67, -6.0F, -7.0F, 0.0F, 6, 7, 0, 0.0F, false));
        glass.cubeList.add(new ModelBox(glass, 0, 59, -6.0F, -7.0F, 0.0F, 6, 0, 8, 0.0F, false));

        bb_main = new ModelRenderer(this);
        bb_main.setRotationPoint(0.0F, 24.0F, 0.0F);
        bb_main.cubeList.add(new ModelBox(bb_main, 0, 0, -8.0F, -3.0F, -8.0F, 16, 3, 16, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 0, 19, -7.0F, -5.0F, -6.0F, 14, 2, 13, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 0, 34, -8.0F, -8.0F, -8.0F, 16, 3, 10, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 44, 47, -8.0F, -8.0F, 2.0F, 16, 3, 6, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 0, 47, -8.0F, -16.0F, 2.0F, 16, 6, 6, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 52, 37, -7.0F, -10.0F, 2.0F, 14, 2, 3, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 44, 56, -8.0F, -16.0F, -8.0F, 3, 8, 10, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 54, 19, 1.0F, -16.0F, -8.0F, 7, 8, 10, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 64, 9, -5.0F, -16.0F, 1.0F, 6, 8, 1, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 52, 42, -4.0F, -10.0F, 5.0F, 8, 2, 2, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 70, 64, -2.0F, -4.0F, 7.0F, 4, 1, 1, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 12, 67, -5.0F, -15.02F, -7.01F, 6, 1, 1, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 26, 67, -8.0F, -5.0F, -6.0F, 1, 2, 4, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 12, 69, 7.0F, -5.0F, -6.0F, 1, 2, 4, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 70, 56, 4.0F, -5.0F, -8.0F, 4, 2, 2, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 70, 60, -8.0F, -5.0F, -8.0F, 4, 2, 2, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 36, 67, 0.0F, -13.0F, -4.0F, 1, 3, 2, 0.0F, false));
        bb_main.cubeList.add(new ModelBox(bb_main, 70, 66, -5.0F, -13.0F, -4.0F, 1, 3, 2, 0.0F, false));
    }

    public void renderItem(float size) {
        renderModel(size,false);
        glass.render(size);
    }

    public void renderModel(float size, boolean isEnableGlow) {
        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        doRender(size);
        GlStateManager.popMatrix();
        if (isEnableGlow) {
            GlStateManager.pushMatrix();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            MekanismRenderer.GlowInfo glowInfo = MekanismRenderer.enableGlow();
            GlStateManager.scale(1.001F, 1.001F, 1.001F);
            GlStateManager.translate(-0.0011F, -0.0011F, -0.0011F);
            port_leds.render(size);
            MekanismRenderer.disableGlow(glowInfo);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }
        if (isEnableGlow) {
            GlStateManager.pushMatrix();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            glass.render(size);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }
    }


    public void renderBloom(float size) {
        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.scale(1.0011F, 1.0011F, 1.0011F);
        GlStateManager.translate(-0.0012F, -0.0012F, -0.0012F);
        MekanismRenderer.GlowInfo glowInfo = MekanismRenderer.enableGlow();
        port_leds.render(size);
        MekanismRenderer.disableGlow(glowInfo);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    public void doRender(float size) {
        port_leds.render(size);
        lights.render(size);
        ports.render(size);
        bb_main.render(size);
    }


    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }
}
