package mekceumoremachine.common.recipe.cache.inputs;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.IExtendedGasTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.cache.inputs.IInputHandler;
import mekanism.common.recipe.inputs.MachineInput;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import mekanism.api.fluid.IExtendedFluidTank;

public final class TemplateInputHelper {

    private TemplateInputHelper() {
    }

    public static IInputHandler<ItemStack, ItemStack> getItemTemplateInputHandler(IInventorySlot slot, RecipeError notEnoughError) {
        return new IInputHandler<>() {

            @Override
            public ItemStack getInput() {
                return slot.getStack();
            }

            @Override
            public ItemStack getRecipeInput(ItemStack recipeIngredient) {
                ItemStack input = getInput();
                return !input.isEmpty() && MachineInput.inputContains(input, recipeIngredient) ? recipeIngredient.copy() : ItemStack.EMPTY;
            }

            @Override
            public void use(ItemStack recipeInput, int operations) {
            }

            @Override
            public void calculateOperationsCanSupport(OperationTracker tracker, ItemStack recipeInput, int usageMultiplier) {
                if (usageMultiplier <= 0) {
                    return;
                }
                if (!recipeInput.isEmpty() && getInput().getCount() >= recipeInput.getCount() * usageMultiplier) {
                    return;
                }
                tracker.resetProgress(notEnoughError);
            }
        };
    }

    public static IInputHandler<GasStack, GasStack> getGasTemplateInputHandler(IExtendedGasTank tank, RecipeError notEnoughError) {
        return new IInputHandler<>() {

            @Override
            public GasStack getInput() {
                GasStack stack = tank.getGas();
                return stack == null ? null : stack.copy();
            }

            @Override
            public GasStack getRecipeInput(GasStack recipeIngredient) {
                GasStack stack = tank.getGas();
                return stack != null && recipeIngredient != null && stack.isGasEqual(recipeIngredient) ? recipeIngredient.copy() : null;
            }

            @Override
            public void use(GasStack recipeInput, int operations) {
            }

            @Override
            public void calculateOperationsCanSupport(OperationTracker tracker, GasStack recipeInput, int usageMultiplier) {
                if (usageMultiplier <= 0) {
                    return;
                }
                if (recipeInput != null && recipeInput.amount > 0 && tank.getStored() >= recipeInput.amount * usageMultiplier) {
                    return;
                }
                tracker.resetProgress(notEnoughError);
            }
        };
    }

    public static IInputHandler<FluidStack, FluidStack> getFluidTemplateInputHandler(IExtendedFluidTank tank, RecipeError notEnoughError) {
        return new IInputHandler<>() {

            @Override
            public FluidStack getInput() {
                FluidStack stack = tank.getFluid();
                return stack == null ? null : stack.copy();
            }

            @Override
            public FluidStack getRecipeInput(FluidStack recipeIngredient) {
                FluidStack stack = tank.getFluid();
                return stack != null && recipeIngredient != null && stack.isFluidEqual(recipeIngredient) ? recipeIngredient.copy() : null;
            }

            @Override
            public void use(FluidStack recipeInput, int operations) {
            }

            @Override
            public void calculateOperationsCanSupport(OperationTracker tracker, FluidStack recipeInput, int usageMultiplier) {
                if (usageMultiplier <= 0) {
                    return;
                }
                if (recipeInput != null && recipeInput.amount > 0 && tank.getFluidAmount() >= recipeInput.amount * usageMultiplier) {
                    return;
                }
                tracker.resetProgress(notEnoughError);
            }
        };
    }
}
