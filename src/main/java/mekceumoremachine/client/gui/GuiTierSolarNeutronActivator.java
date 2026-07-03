package mekceumoremachine.client.gui;

import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekceumoremachine.common.inventory.container.ContainerTierSolarNeutronActivator;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

public class GuiTierSolarNeutronActivator extends GuiConfigurableTile<TileEntityTierSolarNeutronActivator, ContainerTierSolarNeutronActivator> {

    public GuiTierSolarNeutronActivator(InventoryPlayer inventory, TileEntityTierSolarNeutronActivator tile) {
        super(tile, new ContainerTierSolarNeutronActivator(inventory, tile));
        dynamicSlots = true;
        titleLabelY = 4;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiGasGauge(this, tileEntity.inputTank, GuiGasGauge.Type.STANDARD, 25, 13).withColor(GuiGasGauge.GaugeColor.RED));
        addButton(new GuiGasGauge(this, tileEntity.outputTank, GuiGasGauge.Type.STANDARD, 133, 13).withColor(GuiGasGauge.GaugeColor.BLUE));
        addButton(new GuiProgress(() -> {
            if (!tileEntity.getActive() || tileEntity.ticksRequired <= 0) {
                return 0;
            }
            return Math.max(0, Math.min((double) tileEntity.operatingTicks / tileEntity.ticksRequired, 1));
        }, ProgressType.LARGE_RIGHT, this, 64, 39).recipeViewerCategories(RecipeViewerRecipeType.ACTIVATING));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), titleLabelY);
        renderInventoryText(8, ySize - 92, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }
}
