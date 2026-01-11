package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.gui.element.GuiProgress;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.gauge.GuiNumberGauge;
import mekanism.client.gui.element.slot.GuiEnergySlot;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.machines.ReplicatorGasStackRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ReplicatorGasesRecipeCategory<WRAPPER extends ReplicatorGasesRecipeWrapper<ReplicatorGasStackRecipe>> extends BaseRecipeCategory<WRAPPER> {

    public ReplicatorGasesRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", RecipeHandler.Recipe.REPLICATOR_GASES_RECIPE.getJEICategory(), "tile.ReplicatorGases.name", GuiProgress.ProgressBar.LARGE_RIGHT, 4, 4, 169, 69);

    }

    @Override
    protected void addGuiElements() {
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 5, 13).withColor(GuiGauge.TypeColor.RED));
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 26, 13).withColor(GuiGauge.TypeColor.ORANGE));
        guiElements.add(GuiGasGauge.getDummy(GuiGauge.Type.STANDARD, this, guiLocation, 133, 13).withColor(GuiGauge.TypeColor.BLUE));
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
        ReplicatorGasStackRecipe tempRecipe = recipeWrapper.getRecipe();
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initGas(gasStacks, 0, true, 6 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getInput().input, true);
        initGas(gasStacks, 1, true, 27 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getInput().uu, true);
        initGas(gasStacks, 2, false, 134 - xOffset, 14 - yOffset, 16, 58, tempRecipe.getOutput().output, true);
    }
}
