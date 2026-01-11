package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.gui.element.GuiPowerBar;
import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.gui.element.slot.GuiInputSlot;
import mekanism.client.gui.element.slot.GuiOutputSlot;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.machines.ReplicatorItemStackRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;

public class ReplicatorItemStackRecipeCategory<WRAPPER extends ReplicatorItemStackRecipeWrapper<ReplicatorItemStackRecipe>> extends BaseRecipeCategory<WRAPPER> {

    public ReplicatorItemStackRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", RecipeHandler.Recipe.REPLICATOR_ITEMSTACK_RECIPE.getJEICategory(), "tile.ReplicatorItemStack.name", GuiProgress.ProgressBar.MEDIUM, 20, 10, 150, 60);

    }

    @Override
    protected void addGuiElements() {
        guiElements.add(new GuiInputSlot(this, guiLocation, 53, 34));
        guiElements.add(new GuiEnergySlot(this, guiLocation, 140, 34));
        guiElements.add(new GuiOutputSlot(this, guiLocation, 115, 34));
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 28, 10).withColor(GuiGauge.TypeColor.RED));
        guiElements.add(new GuiPowerBar(this, new GuiPowerBar.IPowerInfoHandler() {
            @Override
            public double getLevel() {
                return 1F;
            }
        }, guiLocation, 164, 15));
        guiElements.add(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return (float) timer.getValue() / 20F;
            }
        }, progressBar, this, guiLocation, 75, 37, false));
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WRAPPER recipeWrapper, IIngredients ingredients) {
        ReplicatorItemStackRecipe tempRecipe = recipeWrapper.getRecipe();
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 53 - xOffset, 34 - yOffset);
        itemStacks.init(1, false, 115 - xOffset, 34 - yOffset);
        itemStacks.set(0, tempRecipe.recipeInput.getSolid());
        itemStacks.set(1, tempRecipe.recipeOutput.output);
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initGas(gasStacks, 0, true, 29 - xOffset, 11 - yOffset, 16, 58, tempRecipe.recipeInput.getGas(), true);
    }
}
