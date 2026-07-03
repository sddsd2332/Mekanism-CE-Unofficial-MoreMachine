package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.client.gui.element.tab.GuiSortingTabTierMachine;
import mekceumoremachine.common.inventory.container.ContainerTierNutritionalLiquifier;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierNutritional.TileEntityTierNutritionalLiquifier;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiTierNutritionalLiquifier extends GuiConfigurableTile<TileEntityTierNutritionalLiquifier, ContainerTierNutritionalLiquifier> {

    public GuiTierNutritionalLiquifier(InventoryPlayer inventory, TileEntityTierNutritionalLiquifier tile) {
        super(tile, new ContainerTierNutritionalLiquifier(inventory, tile));
        xSize += tile.tier == MachineTier.ULTIMATE ? 34 : 0;
        ySize += 16;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        int xmove = tileEntity.tier == MachineTier.ULTIMATE ? 34 : 0;
        int slotLocation = tileEntity.tier == MachineTier.BASIC ? 55 : tileEntity.tier == MachineTier.ADVANCED ? 35 : tileEntity.tier == MachineTier.ELITE ? 29 : 27;
        int xDistance = tileEntity.tier == MachineTier.BASIC ? 38 : tileEntity.tier == MachineTier.ADVANCED ? 26 : 19;
        int powerBarY = 13;
        int powerBarHeight = 56 + 30 - powerBarY - 2;
        addButton(new GuiVerticalPowerBar(this, tileEntity, 164 + xmove, powerBarY, powerBarHeight));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.energyPerTick) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy())))));
        addButton(new GuiSortingTabTierMachine<>(this, tileEntity));
        for (int i = 0; i < tileEntity.tier.processes; i++) {
            int cacheIndex = i;
            int gaugeX = slotLocation + i * xDistance;
            int progressX = gaugeX + 4;
            addButton(new GuiGasGauge(this, () -> tileEntity.outPutTanks[cacheIndex], GuiGasGauge.Type.SMALL, gaugeX, 56)
                  .withColor(GuiGasGauge.GaugeColor.BLUE));
            addButton(new GuiProgress(() -> tileEntity.getScaledProgress(cacheIndex), ProgressType.DOWN, this, progressX, 33)
                  .recipeViewerCategories(RecipeViewerRecipeType.NUTRITIONAL_LIQUIFICATION));
        }
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 4);
        renderInventoryText(tileEntity.tier == MachineTier.ULTIMATE ? 27 : 8, ySize - 91, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }
}
