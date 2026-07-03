package mekceumoremachine.client.integration.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
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
import mekanism.common.recipe.machines.ReplicatorFluidStackRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ReplicatorFluidStackRecipeCategory<WRAPPER extends ReplicatorFluidStackRecipeWrapper<ReplicatorFluidStackRecipe>> extends BaseRecipeCategory<WRAPPER> {

    private GuiGauge<?> fluidInput;
    private GuiGauge<?> gasInput;
    private GuiGauge<?> fluidOutput;

    public ReplicatorFluidStackRecipeCategory(IGuiHelper helper) {
        super(helper, "mekanism:gui/Null.png", RecipeHandler.Recipe.REPLICATOR_FLUIDSTACK_RECIPE.getJEICategory(), "tile.ReplicatorFluidStack.name", 4, 4, 169, 69, ProgressType.LARGE_RIGHT);
    }

    @Override
    protected void addGuiElements() {
        fluidInput = addElement(dummyFluidGauge(GuiFluidGauge.Type.STANDARD, GuiFluidGauge.GaugeColor.RED, 5, 13));
        gasInput = addElement(dummyGasGauge(GuiGasGauge.Type.STANDARD, GuiGasGauge.GaugeColor.ORANGE, 26, 13));
        fluidOutput = addElement(dummyFluidGauge(GuiFluidGauge.Type.STANDARD, GuiFluidGauge.GaugeColor.BLUE, 133, 13));
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
        ReplicatorFluidStackRecipe tempRecipe = recipeWrapper.getRecipe();
        IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();
        initFluid(fluidStacks, 0, true, fluidInput, tempRecipe.getInput().ingredientFluid);
        initFluid(fluidStacks, 1, false, fluidOutput, tempRecipe.getOutput().output);
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initGas(gasStacks, 0, true, gasInput, tempRecipe.getInput().ingredientGas);
    }
}
