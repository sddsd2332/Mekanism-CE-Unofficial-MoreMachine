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
import mekceumoremachine.common.inventory.container.ContainerTierChemicalCrystallizer;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierCrystallizer.TileEntityTierChemicalCrystallizer;
import mekceumoremachine.client.gui.element.tab.GuiSortingTabTierMachine;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiTierChemicalCrystallizer extends GuiConfigurableTile<TileEntityTierChemicalCrystallizer, ContainerTierChemicalCrystallizer> {

    public GuiTierChemicalCrystallizer(InventoryPlayer inventory, TileEntityTierChemicalCrystallizer tile) {
        super(tile, new ContainerTierChemicalCrystallizer(inventory, tile));
        xSize += tile.tier == MachineTier.ULTIMATE ? 34 : 0;
        ySize += 13;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        int xmove = tileEntity.tier == MachineTier.ULTIMATE ? 34 : 0;
        int slotLocation = tileEntity.tier == MachineTier.BASIC ? 55 : tileEntity.tier == MachineTier.ADVANCED ? 35 : tileEntity.tier == MachineTier.ELITE ? 29 : 27;
        int xDistance = tileEntity.tier == MachineTier.BASIC ? 38 : tileEntity.tier == MachineTier.ADVANCED ? 26 : 19;
        int powerBarY = 13;
        int powerBarHeight = 71 + 18 - powerBarY - 2;
        addButton(new GuiVerticalPowerBar(this, tileEntity, 164 + xmove, powerBarY, powerBarHeight));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.energyPerTick) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy())))));
        addButton(new GuiSortingTabTierMachine<>(this, tileEntity));
        for (int i = 0; i < tileEntity.tier.processes; i++) {
            int cacheIndex = i;
            int gaugeX = slotLocation + i * xDistance;
            addButton(new GuiGasGauge(this, () -> tileEntity.inputTanks[cacheIndex], GuiGasGauge.Type.SMALL, gaugeX, 13)
                  .withColor(GuiGasGauge.GaugeColor.ORANGE));
            addButton(new GuiProgress(() -> tileEntity.getScaledProgress(cacheIndex), ProgressType.DOWN, this, gaugeX + 4, 47)
                  .recipeViewerCategories(RecipeViewerRecipeType.CRYSTALLIZING));
        }
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 4);
        renderInventoryText(tileEntity.tier == MachineTier.ULTIMATE ? 27 : 8, ySize - 91, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }
}
