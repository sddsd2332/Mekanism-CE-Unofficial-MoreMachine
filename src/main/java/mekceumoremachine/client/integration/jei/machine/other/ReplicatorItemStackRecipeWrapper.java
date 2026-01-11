package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.machine.MekanismRecipeWrapper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.machines.ReplicatorItemStackRecipe;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;

import java.util.ArrayList;
import java.util.List;

public class ReplicatorItemStackRecipeWrapper<RECIPE extends ReplicatorItemStackRecipe> extends MekanismRecipeWrapper<RECIPE> {

    public ReplicatorItemStackRecipeWrapper(RECIPE recipe) {
        super(recipe);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, recipe.getInput().getSolid());
        ingredients.setInput(MekanismJEI.TYPE_GAS, recipe.getInput().getGas());
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getOutput().output);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();
        if (mouseX >= 162 - 17 && mouseX < 166 - 17 && mouseY >= 6 && mouseY < 6 + 52) {
            tooltip.add(LangUtils.localize("gui.using") + ":" + MekanismUtils.convertToDisplay(200D + recipe.extraEnergy) + " " + MekanismConfig.current().general.energyUnit.val() + "/" + recipe.ticks + " " + "tick");
        }
        return tooltip;
    }
}
