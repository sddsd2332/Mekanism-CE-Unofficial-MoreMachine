package mekceumoremachine.client.gui;

import mekanism.client.gui.element.GuiRecipeViewerArea;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.util.LangUtils;
import mekanism.generators.client.gui.GuiGenerator;
import mekceumoremachine.common.inventory.container.ContainerTierGasGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiTierGasGenerator extends GuiGenerator<TileEntityTierGasGenerator, ContainerTierGasGenerator> {

    public GuiTierGasGenerator(InventoryPlayer inventory, TileEntityTierGasGenerator tile) {
        super(tile, new ContainerTierGasGenerator(inventory, tile));
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiEnergyTab(this, () -> getEnergyTabText(tileEntity.generationRate * tileEntity.getUsed() * tileEntity.getMaxBurnTicks())));
        addButton(new GuiGasGauge(this, tileEntity.fuelTank, GuiGasGauge.Type.WIDE, 55, 18));
        addButton(new GuiRecipeViewerArea(this, 55, 18, 66, 50, RecipeViewerRecipeType.GAS_FUEL_TO_ENERGY));
        addButton(new GuiVerticalPowerBar(this, tileEntity.getEnergyContainer(), 164, 15));
    }

    @Override
    protected void renderGeneratorInventoryText() {
        renderInventoryTextAndOther(new TextComponentString(LangUtils.localize("gui.burnRate") + ": " + tileEntity.getUsed()));
    }
}
