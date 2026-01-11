package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.machine.MekanismRecipeWrapper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.machines.ReplicatorGasStackRecipe;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mezz.jei.api.ingredients.IIngredients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReplicatorGasesRecipeWrapper<RECIPE extends ReplicatorGasStackRecipe> extends MekanismRecipeWrapper<RECIPE> {

    public ReplicatorGasesRecipeWrapper(RECIPE recipe) {
        super(recipe);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(MekanismJEI.TYPE_GAS, Arrays.asList(recipe.recipeInput.input, recipe.recipeInput.uu));
        ingredients.setOutput(MekanismJEI.TYPE_GAS, recipe.recipeOutput.output);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();
        if (mouseX >= 155 - 4 && mouseX < 170 -4 && mouseY >= 44 -4 && mouseY < 71 - 4) {
            tooltip.add(LangUtils.localize("gui.using") + ":" + MekanismUtils.convertToDisplay(200D + recipe.extraEnergy) + " " + MekanismConfig.current().general.energyUnit.val() + "/" + recipe.ticks + " " + "tick");
        }
        return tooltip;
    }
}
