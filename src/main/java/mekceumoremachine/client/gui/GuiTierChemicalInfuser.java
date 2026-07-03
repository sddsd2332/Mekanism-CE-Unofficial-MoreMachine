package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerTierChemicalInfuser;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalInfuser;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiTierChemicalInfuser extends GuiConfigurableTile<TileEntityTierChemicalInfuser, ContainerTierChemicalInfuser> {

    private GuiElement centerGauge;

    public GuiTierChemicalInfuser(InventoryPlayer inventory, TileEntityTierChemicalInfuser tile) {
        super(tile, new ContainerTierChemicalInfuser(inventory, tile));
        inventoryLabelY += 2;
        titleLabelY = 5;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiHorizontalPowerBar(this, tileEntity, 115, 75));
        addButton(new GuiEnergyTab(this, () -> Arrays.asList(
              new TextComponentString(LangUtils.localize("gui.using") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.clientEnergyUsed) + "/t"),
              new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy())))));
        addButton(new GuiGasGauge(this, tileEntity.leftTank, GuiGasGauge.Type.STANDARD, 25, 13).withColor(GuiGasGauge.GaugeColor.RED));
        centerGauge = addButton(new GuiGasGauge(this, tileEntity.centerTank, GuiGasGauge.Type.STANDARD, 79, 4).withColor(GuiGasGauge.GaugeColor.BLUE));
        addButton(new GuiGasGauge(this, tileEntity.rightTank, GuiGasGauge.Type.STANDARD, 133, 13).withColor(GuiGasGauge.GaugeColor.ORANGE));
        addButton(new GuiProgress(() -> tileEntity.getActive() ? tileEntity.getScaledProgress() : 0, ProgressType.SMALL_RIGHT, this, 47, 39)
              .recipeViewerCategories(RecipeViewerRecipeType.CHEMICAL_INFUSING));
        addButton(new GuiProgress(() -> tileEntity.getActive() ? tileEntity.getScaledProgress() : 0, ProgressType.SMALL_LEFT, this, 101, 39)
              .recipeViewerCategories(RecipeViewerRecipeType.CHEMICAL_INFUSING));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleTextWithOffset(new TextComponentString(tileEntity.getName()), 1, titleLabelY,
              centerGauge == null ? getXSize() : centerGauge.getRelativeX(), 4, TextAlignment.LEFT);
        renderInventoryText(centerGauge == null ? getXSize() : centerGauge.getRelativeX());
        super.drawForegroundText(mouseX, mouseY);
    }
}
