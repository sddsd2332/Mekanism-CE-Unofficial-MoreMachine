package mekceumoremachine.common;

import mekanism.api.gas.GasStack;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.MekanismFluids;
import mekanism.common.MekanismItems;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.CompositeInput;
import mekanism.common.util.StackUtils;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.ForgeRegistry;

public class MEKCeuMoreMachineRecipes {

    public static void addRecipes() {

    }


    public static void removeRecipes() {
        /**
         * @author sddsd2332
         * @reason 移除化学清洗机对应的工厂配方
         */
        removeMekRecipes("machineblock_5_23");
        removeMekRecipes("machineblock_6_23");
        removeMekRecipes("machineblock_7_23");
        removeMekRecipes("machineblock3_7_23");

        /**
         * @author sddsd2332
         * @reason 移除大型风力发电机的对应配方
         */
        ItemStack EnergyCubeStack = new ItemStack(MekanismBlocks.EnergyCube, 4);
        if (!EnergyCubeStack.hasTagCompound()) {
            EnergyCubeStack.setTagCompound(new NBTTagCompound());
        }
        EnergyCubeStack.getTagCompound().setInteger("tier", 3);

        OreDictionary.getOres("blockSteel", false).forEach(blockSteel -> {
            OreDictionary.getOres("blockOsmium", false).forEach(blockOsmium -> Recipe.DIGITAL_ASSEMBLY_TABLE.remove(Recipe.DIGITAL_ASSEMBLY_TABLE.get().get(new CompositeInput(StackUtils.size(blockOsmium, 36), StackUtils.size(blockSteel, 64), StackUtils.size(blockOsmium, 36),
                    new ItemStack(MekanismBlocks.BasicBlock, 36, 8), new ItemStack(MekanismItems.AtomicAlloy, 36), new ItemStack(MekanismBlocks.BasicBlock, 36, 8),
                    EnergyCubeStack, new ItemStack(MekanismItems.ControlCircuit, 16, 3), EnergyCubeStack,
                    FluidRegistry.getFluidStack("water", 20000), new GasStack(MekanismFluids.Oxygen, 10000)))
            ));
        });

        /**
         * @author sddsd2332
         * @reason 添加本mod的大型风力发电机配方
         */
        OreDictionary.getOres("blockSteel", false).forEach(blockSteel -> {
            OreDictionary.getOres("blockOsmium", false).forEach(blockOsmium -> RecipeHandler.addDigitalAssemblyTableRecipe(
                    StackUtils.size(blockOsmium, 36), StackUtils.size(blockSteel, 64), StackUtils.size(blockOsmium, 36),
                    new ItemStack(MekanismBlocks.BasicBlock, 36, 8), new ItemStack(MekanismItems.AtomicAlloy, 36), new ItemStack(MekanismBlocks.BasicBlock, 36, 8),
                    EnergyCubeStack, new ItemStack(MekanismItems.ControlCircuit, 16, 3), EnergyCubeStack,
                    FluidRegistry.getFluidStack("water", 20000), new GasStack(MekanismFluids.Oxygen, 10000),
                    new ItemStack(MEKCeuMoreMachineBlocks.BigWindGenerator, 1), FluidRegistry.getFluidStack("water", 1000), new GasStack(MekanismFluids.Hydrogen, 1000),
                    100, 2400));
        });



    }

    public static void removeMekRecipes(String recipeName) {
        removeRecipes(Mekanism.MODID, recipeName);
    }

    public static void removeRecipes(String modid, String recipeName) {
        ForgeRegistry<IRecipe> recipeRegistry = (ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES;
        ResourceLocation recipe = new ResourceLocation(modid, recipeName);
        recipeRegistry.remove(recipe);
    }
}
