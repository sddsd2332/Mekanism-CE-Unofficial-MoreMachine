package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.gauge.GuiNumberGauge;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.machines.ReplicatorFluidStackRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ReplicatorFluidStackRecipeCategory<WRAPPER extends ReplicatorFluidStackRecipeWrapper<ReplicatorFluidStackRecipe>> extends BaseRecipeCategory<WRAPPER> {

    public ReplicatorFluidStackRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", RecipeHandler.Recipe.REPLICATOR_FLUIDSTACK_RECIPE.getJEICategory(), "tile.ReplicatorFluidStack.name", GuiProgress.ProgressBar.LARGE_RIGHT, 4, 4, 169, 69);

    }

    @Override
    protected void addGuiElements() {
        guiElements.add(GuiFluidGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 5, 13).withColor(GuiGauge.TypeColor.RED));
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 26, 13).withColor(GuiGauge.TypeColor.ORANGE));
        guiElements.add(GuiFluidGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 133, 13).withColor(GuiGauge.TypeColor.BLUE));
        guiElements.add(new GuiEnergySlot(this, guiLocation, 154, 13));
        guiElements.add(new GuiNumberGauge(new GuiNumberGauge.INumberInfoHandler() {
            @Override
            public TextureAtlasSprite getIcon() {
                return MekanismRenderer.energyIcon;
            }

            @Override
            public double getLevel() {
                return 1D;
            }

            @Override
            public double getMaxLevel() {
                return 1D;
            }

            @Override
            public String getText(double level) {
                return "";
            }
        }, GuiGauge.Type.SMALL, this, guiLocation, 154, 43));

        guiElements.add(new GuiProgress(new GuiProgress.IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return (float) timer.getValue() / 20F;
            }
        }, progressBar, this, guiLocation, 62, 38, false));
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WRAPPER recipeWrapper, IIngredients ingredients) {
        ReplicatorFluidStackRecipe tempRecipe = recipeWrapper.getRecipe();
        IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();
        fluidStacks.init(0, true, 6 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getInput().ingredientFluid.amount, false, fluidOverlayLarge);
        fluidStacks.set(0, tempRecipe.recipeInput.ingredientFluid);

        fluidStacks.init(1, false, 134 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getOutput().output.amount, false, fluidOverlayLarge);
        fluidStacks.set(1, tempRecipe.recipeOutput.output);

        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initGas(gasStacks, 0, true, 27 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getInput().ingredientGas, true);
    }
}
