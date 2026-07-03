package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiReplicatorGases extends GuiConfigurableTile<TileEntityReplicatorGases, ContainerReplicatorGases> {

    public GuiReplicatorGases(InventoryPlayer inventory, TileEntityReplicatorGases tile) {
        super(tile, new ContainerReplicatorGases(inventory, tile));
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiEnergyTab(this, () -> {
            double extra = tileEntity.getRecipe() != null ? tileEntity.getRecipe().extraEnergy : 0;
            String using = MekanismUtils.getEnergyDisplay(MekanismUtils.getEnergyPerTick(tileEntity, tileEntity.BASE_ENERGY_PER_TICK + extra));
            return Arrays.asList(
                  new TextComponentString(LangUtils.localize("gui.using") + ": " + using + "/t"),
                  new TextComponentString(LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getNeedEnergy())));
        }));
        addButton(new GuiGasGauge(this, tileEntity.inputTank, GuiGasGauge.Type.STANDARD, 5, 13).withColor(GuiGasGauge.GaugeColor.RED));
        addButton(new GuiGasGauge(this, tileEntity.uuTank, GuiGasGauge.Type.STANDARD, 26, 13).withColor(GuiGasGauge.GaugeColor.ORANGE));
        addButton(new GuiGasGauge(this, tileEntity.outputTank, GuiGasGauge.Type.STANDARD, 133, 13).withColor(GuiGasGauge.GaugeColor.BLUE));
        addButton(new GuiEnergyGauge(this, tileEntity, GuiEnergyGauge.Type.SMALL, 154, 43));
        addButton(new GuiProgress(tileEntity::getScaledProgress, ProgressType.LARGE_RIGHT, this, 64, 39)
              .recipeViewerCategories(RecipeViewerRecipeType.simple(RecipeHandler.Recipe.REPLICATOR_GASES_RECIPE.getJEICategory())));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 6);
        renderInventoryText(8, ySize - 92, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }
}
