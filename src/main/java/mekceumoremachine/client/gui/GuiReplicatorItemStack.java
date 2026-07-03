package mekceumoremachine.client.gui;

import java.util.Arrays;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.inventory.container.ContainerReplicatorItemStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiReplicatorItemStack extends GuiConfigurableTile<TileEntityReplicatorItemStack, ContainerReplicatorItemStack> {

    public GuiReplicatorItemStack(InventoryPlayer inventory, TileEntityReplicatorItemStack tile) {
        super(tile, new ContainerReplicatorItemStack(inventory, tile));
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
        addButton(new GuiGasGauge(this, tileEntity.inputGasTank, GuiGasGauge.Type.STANDARD, 28, 10).withColor(GuiGasGauge.GaugeColor.RED));
        addButton(new GuiVerticalPowerBar(this, tileEntity, 164, 15));
        addButton(new GuiProgress(tileEntity::getScaledProgress, ProgressType.BAR, this, 75, 37)
              .recipeViewerCategories(RecipeViewerRecipeType.simple(RecipeHandler.Recipe.REPLICATOR_ITEMSTACK_RECIPE.getJEICategory())));
    }

    @Override
    protected void drawForegroundText(int mouseX, int mouseY) {
        drawTitleText(new TextComponentString(tileEntity.getName()), 6);
        renderInventoryText(8, ySize - 94, getXSize());
        super.drawForegroundText(mouseX, mouseY);
    }
}
