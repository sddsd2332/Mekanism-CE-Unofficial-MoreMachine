package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.machine.MekanismRecipeWrapper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.machines.ReplicatorFluidStackRecipe;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;

import java.util.ArrayList;
import java.util.List;

public class ReplicatorFluidStackRecipeWrapper<RECIPE extends ReplicatorFluidStackRecipe> extends MekanismRecipeWrapper<RECIPE> {

    public ReplicatorFluidStackRecipeWrapper(RECIPE recipe) {
        super(recipe);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, recipe.recipeInput.ingredientFluid);
        ingredients.setInput(MekanismJEI.TYPE_GAS, recipe.recipeInput.ingredientGas);
        ingredients.setOutput(VanillaTypes.FLUID, recipe.recipeOutput.output);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();
        if (mouseX >= 155 - 4 && mouseX < 170 - 4 && mouseY >= 44 - 4 && mouseY < 71 - 4) {
            tooltip.add(LangUtils.localize("gui.using") + ":" + MekanismUtils.convertToDisplay(200D + recipe.extraEnergy) + " " + MekanismConfig.current().general.energyUnit.val() + "/" + recipe.ticks + " " + "tick");
        }
        return tooltip;
    }
}
