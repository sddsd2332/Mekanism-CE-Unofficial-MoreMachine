package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiNumberGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.machines.ReplicatorGasStackRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ReplicatorGasesRecipeCategory<WRAPPER extends ReplicatorGasesRecipeWrapper<ReplicatorGasStackRecipe>> extends BaseRecipeCategory<WRAPPER> {

    private GuiGauge<?> gasInput;
    private GuiGauge<?> uuInput;
    private GuiGauge<?> gasOutput;

    public ReplicatorGasesRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", RecipeHandler.Recipe.REPLICATOR_GASES_RECIPE.getJEICategory(), "tile.ReplicatorGases.name", 4, 4, 169, 69, ProgressType.LARGE_RIGHT);
    }

    @Override
    protected void addGuiElements() {
        gasInput = addElement(dummyGasGauge(GuiGasGauge.Type.STANDARD, GuiGasGauge.GaugeColor.RED, 5, 13));
        uuInput = addElement(dummyGasGauge(GuiGasGauge.Type.STANDARD, GuiGasGauge.GaugeColor.ORANGE, 26, 13));
        gasOutput = addElement(dummyGasGauge(GuiGasGauge.Type.STANDARD, GuiGasGauge.GaugeColor.BLUE, 133, 13));
        guiElements.add(new GuiSlot(SlotType.POWER, this, 154, 13).with(SlotOverlay.POWER).setRenderAboveSlots());

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
            public double getScaledLevel() {
                return 1D;
            }

            @Override
            public String getText() {
                return "";
            }
        }, GaugeType.SMALL, this, 154, 43));

        guiElements.add(new GuiProgress(new IProgressInfoHandler() {
            @Override
            public double getProgress() {
                return (double) timer.getValue() / 20F;
            }

            @Override
            public boolean isGuiInJei() {
                return true;
            }
        }, progressType, this, 62, 38));
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WRAPPER recipeWrapper, IIngredients ingredients) {
        ReplicatorGasStackRecipe tempRecipe = recipeWrapper.getRecipe();
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initGas(gasStacks, 0, true, gasInput, tempRecipe.getInput().input);
        initGas(gasStacks, 1, true, uuInput, tempRecipe.getInput().uu);
        initGas(gasStacks, 2, false, gasOutput, tempRecipe.getOutput().output);
    }
}
