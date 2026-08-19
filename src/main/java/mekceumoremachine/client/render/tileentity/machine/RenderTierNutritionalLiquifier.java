package mekceumoremachine.client.render.tileentity.machine;

import mekanism.api.gas.GasStack;
import mekanism.client.render.GasRenderMap;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelTierNutritionalLiquifier;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.TierNutritional.TileEntityTierNutritionalLiquifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class RenderTierNutritionalLiquifier extends TileEntitySpecialRenderer<TileEntityTierNutritionalLiquifier> {

    public static final RenderTierNutritionalLiquifier INSTANCE = new RenderTierNutritionalLiquifier();

    private static GasRenderMap<MekanismRenderer.DisplayInteger[]> cachedCenterGas = new GasRenderMap<>();

    private static final int stages = 64;

    private ModelTierNutritionalLiquifier model = new ModelTierNutritionalLiquifier();

    public static void resetDisplayInts() {
        cachedCenterGas.values().forEach(MekanismRenderer.DisplayInteger::deleteAll);
        cachedCenterGas.clear();
    }

    @Override
    public void render(TileEntityTierNutritionalLiquifier tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        ItemStack stack = tileEntity.getStackInSlot(2);

        if (!stack.isEmpty() && tileEntity.outputTank1.getStored() < tileEntity.outputTank1.getMaxGas() / 2) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5D, y + 0.65D, z + 0.5D);
            float scale = stack.getItem() instanceof ItemBlock ? 0.425F : 0.325F;
            GlStateManager.scale(scale, scale, scale);
            double tick = Minecraft.getSystemTime() / 800.0D;
            GlStateManager.translate(0.0D, Math.sin(tick % (2 * Math.PI)) * 0.065D, 0.0D);
            GlStateManager.rotate((float) (((tick * 40.0D) % 360)), 0, 1, 0);
            GlStateManager.disableLighting();
            GlStateManager.pushAttrib();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popAttrib();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }

        if (tileEntity.outputTank1.getStored() > 0) {
            GlStateManager.pushMatrix();
            GlStateManager.enableCull();
            GlStateManager.disableLighting();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.translate((float) x, (float) y, (float) z);
            MekanismRenderer.GlowInfo glowInfo = MekanismRenderer.enableGlow();
            MekanismRenderer.DisplayInteger[] displayList = getListAndRender(tileEntity.outputTank1.getGas());
            MekanismRenderer.color(tileEntity.outputTank1.getGas());
            displayList[Math.min(stages - 1, (int) (tileEntity.prevScale * ((float) stages - 1)))].render();
            MekanismRenderer.resetColor();
            MekanismRenderer.disableGlow(glowInfo);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableLighting();
            GlStateManager.disableCull();
            GlStateManager.popMatrix();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);

        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID, MekanismUtils.ResourceType.RENDER, "NutritionalLiquifier_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderTier(0.0625F);
        bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.RENDER, "NutritionalLiquifier.png"));
        model.render(0.0625F, tileEntity.getActive());
        if (tileEntity.getActive()) {
            double tick = Minecraft.getSystemTime() / 800.0D;
            GlStateManager.rotate((float) (((tick * 100.0D) % 360)), 0, 1, 0);
            model.renderBlade(0.0625F);
        }
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        MekanismRenderer.rotate(tileEntity.facing, 0, 180, 90, 270);
        GlStateManager.rotate(180, 0, 0, 1);
        model.renderGlass(0.0625F, true);

        GlStateManager.popMatrix();
        MekanismRenderer.machineRenderer().render(tileEntity, x, y, z, partialTick, destroyStage, alpha);

    }


    @SuppressWarnings("incomplete-switch")
    private MekanismRenderer.DisplayInteger[] getListAndRender(GasStack gasStack) {
        if (cachedCenterGas.containsKey(gasStack)) {
            return cachedCenterGas.get(gasStack);
        }

        MekanismRenderer.Model3D toReturn = new MekanismRenderer.Model3D();
        toReturn.baseBlock = Blocks.WATER;
        toReturn.setTexture(gasStack.getGas().getSprite());
        MekanismRenderer.DisplayInteger[] displays = new MekanismRenderer.DisplayInteger[stages];
        cachedCenterGas.put(gasStack, displays);

        for (int i = 0; i < stages; i++) {
            displays[i] = MekanismRenderer.DisplayInteger.createAndStart();

            toReturn.minZ = 0.0625 + .01;
            toReturn.maxZ = 0.9375 - .01;

            toReturn.minX = 0.0625 + .01;
            toReturn.maxX = 0.9375 - .01;

            toReturn.minY = 0.3125 + .01;
            toReturn.maxY = 0.3125 + ((float) i / stages) * 0.625 - .01;

            MekanismRenderer.renderObject(toReturn);
            MekanismRenderer.DisplayInteger.endList();
        }
        return displays;
    }
}
