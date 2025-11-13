package mekceumoremachine.common;

import mekanism.api.gas.GasStack;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.MekanismFluids;
import mekanism.common.MekanismItems;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.util.StackUtils;
import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
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
        removeMekFactoryRecipes(23);
        /**
         * @author sddsd2332
         * @reason 移除化学溶解室对应的工厂配方
         */
        removeMekFactoryRecipes(19);
        /**
         * @author sddsd2332
         * @reason 移除化学氧化机对应的工厂配方
         */
        removeMekFactoryRecipes(21);

        removeMekMultiBlockRecipes("largeelectrolyticseparator");
        removeMekMultiBlockRecipes("largechemicalwasher");
        removeMekMultiBlockRecipes("largechemicalinfuser");
        removeMekMultiBlockRecipes("largegasgenerator");
        removeMekMultiBlockRecipes("largewindgenerator");

    }

    public static void removeMekFactoryRecipes(int id) {
        removeMekRecipes("machineblock_5_" + id);
        removeMekRecipes("machineblock_6_" + id);
        removeMekRecipes("machineblock_7_" + id);
        removeMekRecipes("machineblock3_7_" + id);
    }

    public static void removeMekRecipes(String recipeName) {
        removeRecipes(Mekanism.MODID, recipeName);
    }

    public static void removeMekMultiBlockRecipes(String recipeName) {
        removeRecipes(MekanismMultiblockMachine.MODID, recipeName);
    }

    public static void removeRecipes(String modid, String recipeName) {
        ForgeRegistry<IRecipe> recipeRegistry = (ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES;
        ResourceLocation recipe = new ResourceLocation(modid, recipeName);
        recipeRegistry.remove(recipe);
    }
}
