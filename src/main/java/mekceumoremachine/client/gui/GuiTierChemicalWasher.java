package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.GuiSideHolder;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerTierChemicalWasher;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalWasher;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiTierChemicalWasher extends GuiConfigurableTile<TileEntityTierChemicalWasher, ContainerTierChemicalWasher> {

    public GuiTierChemicalWasher(InventoryPlayer inventory, TileEntityTierChemicalWasher tile) {
        super(tile, new ContainerTierChemicalWasher(inventory, tile));
        dynamicSlots = true;
        titleLabelY = 4;
    }

    @Override
    protected void addGuiElements() {
        addButton(GuiSideHolder.create(this, xSize, 66, 57, false, true, SpecialColors.TAB_CHEMICAL_WASHER));
        super.addGuiElements();
        addButton(new GuiDownArrow(this, xSize + 8, 91));
        addButton(new GuiHorizontalPowerBar(this, tileEntity, 113, 74));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.clientEnergyUsed) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy())))));
        addButton(new GuiFluidGauge(() -> tileEntity.fluidTank, () -> tileEntity.getFluidTanks(null), GaugeType.STANDARD, this, 7, 13)
              .withColor(GuiFluidGauge.GaugeColor.RED));
        addButton(new GuiGasGauge(() -> tileEntity.inputTank, () -> tileEntity.getGasTanks(null), GaugeType.STANDARD, this, 28, 13)
              .withColor(GuiGasGauge.GaugeColor.RED));
        addButton(new GuiGasGauge(() -> tileEntity.outputTank, () -> tileEntity.getGasTanks(null), GaugeType.STANDARD, this, 131, 13)
              .withColor(GuiGasGauge.GaugeColor.BLUE));
        addButton(new GuiProgress(() -> tileEntity.getActive() ? 1 : 0, ProgressType.LARGE_RIGHT, this, 64, 39)
              .recipeViewerCategories(RecipeViewerRecipeType.WASHING));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), titleLabelY);
        super.drawForegroundText(mouseX, mouseY);
    }
}
