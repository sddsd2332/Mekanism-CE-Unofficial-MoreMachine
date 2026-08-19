package mekceumoremachine.client.render.tileentity.machine;

import mekanism.api.gas.GasStack;
import mekanism.client.render.GasRenderMap;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.model.machine.ModelTierChemicalDissolutionChamber;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.TierDissolution.TileEntityTierChemicalDissolutionChamber;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import org.lwjgl.opengl.GL11;

public class RenderTierChemicalDissolutionChamber extends TileEntitySpecialRenderer<TileEntityTierChemicalDissolutionChamber> {

    public static final RenderTierChemicalDissolutionChamber INSTANCE = new RenderTierChemicalDissolutionChamber();

    private static GasRenderMap<MekanismRenderer.DisplayInteger[]> cachedCenterGas = new GasRenderMap<>();

    private static final int stages = 64;

    private ModelTierChemicalDissolutionChamber model = new ModelTierChemicalDissolutionChamber();

    public static void resetDisplayInts() {
        cachedCenterGas.values().forEach(MekanismRenderer.DisplayInteger::deleteAll);
        cachedCenterGas.clear();
    }

    @Override
    public void render(TileEntityTierChemicalDissolutionChamber tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        if (tileEntity.injectTank.getStored() > 0) {
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
            MekanismRenderer.DisplayInteger[] displayList = getListAndRender(tileEntity.injectTank.getGas());
            MekanismRenderer.color(tileEntity.injectTank.getGas());
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
        bindTexture(MekanismUtils.getResource(MEKCeuMoreMachine.MODID,MekanismUtils.ResourceType.RENDER,"ChemicalDissolutionChamber_" + tileEntity.tier.getBaseTier().getSimpleName() + ".png"));
        model.renderTier(0.0625F);
        bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.RENDER, "ChemicalDissolutionChamber.png"));
        model.render(0.0625F,true);
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
            toReturn.minZ = 0.125 + .01;
            toReturn.maxZ = 0.875 - .01;
            toReturn.minX = 0.125 + .01;
            toReturn.maxX = 0.875 - .01;
            toReturn.minY = 0.4375 + .01;
            toReturn.maxY = 0.4375 + ((float) i / stages) * 0.3125 - .01;

            MekanismRenderer.renderObject(toReturn);
            MekanismRenderer.DisplayInteger.endList();
        }
        return displays;
    }
}
