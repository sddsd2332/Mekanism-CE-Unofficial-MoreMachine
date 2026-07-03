package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerTierIsotopicCentrifuge;
import mekceumoremachine.common.tile.machine.TileEntityTierIsotopicCentrifuge;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiTierIsotopicCentrifuge extends GuiConfigurableTile<TileEntityTierIsotopicCentrifuge, ContainerTierIsotopicCentrifuge> {

    public GuiTierIsotopicCentrifuge(InventoryPlayer inventory, TileEntityTierIsotopicCentrifuge tile) {
        super(tile, new ContainerTierIsotopicCentrifuge(inventory, tile));
        dynamicSlots = true;
        titleLabelY = 4;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiHorizontalPowerBar(this, tileEntity, 113, 74));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.clientEnergyUsed) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy())))));
        addButton(new GuiGasGauge(this, tileEntity.inputTank, GuiGasGauge.Type.STANDARD, 25, 13).withColor(GuiGasGauge.GaugeColor.RED));
        addButton(new GuiGasGauge(this, tileEntity.outputTank, GuiGasGauge.Type.STANDARD, 133, 13).withColor(GuiGasGauge.GaugeColor.BLUE));
        addButton(new GuiProgress(() -> tileEntity.getActive() ? tileEntity.getScaledProgress() : 0, ProgressType.LARGE_RIGHT, this, 64, 39)
              .recipeViewerCategories(RecipeViewerRecipeType.CENTRIFUGING));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), titleLabelY);
        renderInventoryText(8, ySize - 92, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }
}
