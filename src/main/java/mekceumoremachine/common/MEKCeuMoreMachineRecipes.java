package mekceumoremachine.common;

import mekanism.api.gas.GasStack;
import mekanism.common.Mekanism;
import mekanism.common.MekanismFluids;
import mekanism.common.MekanismItems;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.multiblockmachine.common.MekanismMultiblockMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineFluids;
import mekceumoremachine.common.registries.MEKCeuMoreMachineItems;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

public class MEKCeuMoreMachineRecipes {

    public static void addRecipes() {
        //UU物质（物品） -> 氧化 ->UU物质(气体)
        RecipeHandler.addChemicalOxidizerRecipe(new ItemStack(MEKCeuMoreMachineItems.UUMatter), new GasStack(MEKCeuMoreMachineFluids.UU_MATTER, 500));

        //uu物质回收 产出64个空晶体
        ItemStack Matter = new ItemStack(MEKCeuMoreMachineItems.UUMatter);
        if (RecipeHandler.Recipe.RECYCLER.containsRecipe(Matter)) {
            RecipeHandler.Recipe.RECYCLER.remove(RecipeHandler.Recipe.RECYCLER.get().get(new ItemStackInput(Matter)));
            RecipeHandler.addRecyclerRecipe(Matter, new ItemStack(MekanismItems.EmptyCrystals, 64), 1);
        }
        //生成UU物质.需要64个空晶体和2个反物质气体
        RecipeHandler.addNucleosynthesizerRecipe(new ItemStack(MekanismItems.EmptyCrystals, 64), new GasStack(MekanismFluids.Antimatter, 2), new ItemStack(MEKCeuMoreMachineItems.UUMatter), 0, 500);

        //测试配方 钻石
        RecipeHandler.addItemReplicatorRecipe(new ItemStack(Items.DIAMOND), new GasStack(MEKCeuMoreMachineFluids.UU_MATTER, 20), 0, 200);
        //测试配方 反物质
        RecipeHandler.addGasReplicatorRecipe(new GasStack(MekanismFluids.Antimatter, 1000), new GasStack(MEKCeuMoreMachineFluids.UU_MATTER, 20), 0, 200);
        //测试配方 乙烯(液体)
        RecipeHandler.addFluidReplicatorRecipe(new FluidStack(MekanismFluids.Ethene.getFluid(), 1000), new GasStack(MEKCeuMoreMachineFluids.UU_MATTER, 20), 0, 200);
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
        removeMekMultiBlockRecipes("largesolarneutronactivator");

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
