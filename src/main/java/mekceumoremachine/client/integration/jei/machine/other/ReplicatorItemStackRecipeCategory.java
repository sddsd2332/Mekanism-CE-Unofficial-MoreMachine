package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.machines.ReplicatorItemStackRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;

public class ReplicatorItemStackRecipeCategory<WRAPPER extends ReplicatorItemStackRecipeWrapper<ReplicatorItemStackRecipe>> extends BaseRecipeCategory<WRAPPER> {

    private GuiSlot itemInput;
    private GuiSlot itemOutput;
    private GuiGauge<?> gasInput;

    public ReplicatorItemStackRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", RecipeHandler.Recipe.REPLICATOR_ITEMSTACK_RECIPE.getJEICategory(), "tile.ReplicatorItemStack.name", 20, 10, 150, 60, ProgressType.BAR);
    }

    @Override
    protected void addGuiElements() {
        itemInput = addElement(new GuiSlot(SlotType.INPUT, this, 53, 34).setRenderAboveSlots());
        guiElements.add(new GuiSlot(SlotType.POWER, this, 140, 34).with(SlotOverlay.POWER).setRenderAboveSlots());
        itemOutput = addElement(new GuiSlot(SlotType.OUTPUT, this, 115, 34).setRenderAboveSlots());
        gasInput = addElement(dummyGasGauge(GuiGasGauge.Type.STANDARD, GuiGasGauge.GaugeColor.RED, 28, 10));
        guiElements.add(new GuiVerticalPowerBar(this, () -> 1F, 164, 15));
        guiElements.add(new GuiProgress(new IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return (double) timer.getValue() / 20F;
            }

            @Override
            public boolean isGuiInJei() {
                return true;
            }
        }, progressType, this, 75, 37));
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WRAPPER recipeWrapper, IIngredients ingredients) {
        ReplicatorItemStackRecipe tempRecipe = recipeWrapper.getRecipe();
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        initItem(itemStacks, 0, true, itemInput, tempRecipe.recipeInput.getSolid());
        initItem(itemStacks, 1, false, itemOutput, tempRecipe.recipeOutput.output);
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initGas(gasStacks, 0, true, gasInput, tempRecipe.recipeInput.getGas());
    }
}
