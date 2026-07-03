package mekceumoremachine.client.integration.jei;

import mekanism.common.base.ITierItem;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.MachineInput;
import mekanism.common.recipe.machines.MachineRecipe;
import mekanism.common.recipe.outputs.MachineOutput;
import mekceumoremachine.client.integration.jei.machine.other.ReplicatorFluidStackRecipeWrapper;
import mekceumoremachine.client.integration.jei.machine.other.ReplicatorGasesRecipeWrapper;
import mekceumoremachine.client.integration.jei.machine.other.ReplicatorItemStackRecipeWrapper;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tier.MachineTier;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.recipe.IRecipeWrapperFactory;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import java.util.stream.Collectors;

public class MEKCeuMoreMachineRecipeRegistryHelper {

    public static void registerTierIsotopicCentrifuge(IModRegistry registry) {
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge, Recipe.ISOTOPIC_CENTRIFUGE.getJEICategory());
    }

    public static void registerTierRotaryCondensentrator(IModRegistry registry) {
        String condensentrating = "mekanism.rotary_condensentrator_condensentrating";
        String decondensentrating = "mekanism.rotary_condensentrator_decondensentrating";
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierRotaryCondensentrator, condensentrating, decondensentrating);
    }

    public static void registerTierElectrolyticSeparator(IModRegistry registry) {
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierElectrolyticSeparator, Recipe.ELECTROLYTIC_SEPARATOR.getJEICategory());
    }

    public static void registerTierNeutronActivator(IModRegistry registry) {
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierSolarNeutronActivator, Recipe.SOLAR_NEUTRON_ACTIVATOR.getJEICategory());
    }

    public static void registerTierChemicalInfuser(IModRegistry registry) {
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierChemicalInfuser, Recipe.CHEMICAL_INFUSER.getJEICategory());
    }

    public static void registerTierAmbientAccumulator(IModRegistry registry) {
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierAmbientAccumulator, Recipe.AMBIENT_ACCUMULATOR.getJEICategory());
    }

    public static void registerTierWasher(IModRegistry registry) {
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierChemicalWasher, Recipe.CHEMICAL_WASHER.getJEICategory());
    }

    public static void registerTierChemicalDissolutionChamber(IModRegistry registry) {
        for (MachineTier tier : MachineTier.values()) {
            registry.addRecipeCatalyst(getTierFactory(MEKCeuMoreMachineBlocks.TierChemicalDissolutionChamber, tier), Recipe.CHEMICAL_DISSOLUTION_CHAMBER.getJEICategory());
        }
    }

    public static void registerTierNutritionalLiquifier(IModRegistry registry) {
        for (MachineTier tier : MachineTier.values()) {
            registry.addRecipeCatalyst(getTierFactory(MEKCeuMoreMachineBlocks.TierNutritionalLiquifier, tier), Recipe.NUTRITIONAL_LIQUIFIER.getJEICategory());
        }
    }

    public static void registerTierChemicalOxidizer(IModRegistry registry) {
        for (MachineTier tier : MachineTier.values()) {
            registry.addRecipeCatalyst(getTierFactory(MEKCeuMoreMachineBlocks.TierChemicalOxidizer, tier), Recipe.CHEMICAL_OXIDIZER.getJEICategory());
        }
    }

    public static void registerTierChemicalCrystallizer(IModRegistry registry) {
        for (MachineTier tier : MachineTier.values()) {
            registry.addRecipeCatalyst(getTierFactory(MEKCeuMoreMachineBlocks.TierChemicalCrystallizer, tier), Recipe.CHEMICAL_CRYSTALLIZER.getJEICategory());
        }
    }

    public static void registerGasStackFlueToEnergyRecipe(IModRegistry registry) {
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierGasGenerator, Recipe.GAS_FUEL_TO_ENERGY_RECIPE.getJEICategory());
    }

    public static void registerReplicatorItemStackRecipe(IModRegistry registry) {
        addRecipes(registry, Recipe.REPLICATOR_ITEMSTACK_RECIPE, ReplicatorItemStackRecipeWrapper::new);
        registry.addRecipeCatalyst(new ItemStack(MEKCeuMoreMachineBlocks.ReplicatorItemStack), Recipe.REPLICATOR_ITEMSTACK_RECIPE.getJEICategory());
    }

    public static void registerReplicatorGasesRecipe(IModRegistry registry) {
        addRecipes(registry, Recipe.REPLICATOR_GASES_RECIPE, ReplicatorGasesRecipeWrapper::new);
        registry.addRecipeCatalyst(new ItemStack(MEKCeuMoreMachineBlocks.ReplicatorGases), Recipe.REPLICATOR_GASES_RECIPE.getJEICategory());
    }

    public static void registerReplicatorFluidStackRecipe(IModRegistry registry) {
        addRecipes(registry, Recipe.REPLICATOR_FLUIDSTACK_RECIPE, ReplicatorFluidStackRecipeWrapper::new);
        registry.addRecipeCatalyst(new ItemStack(MEKCeuMoreMachineBlocks.ReplicatorFluidStack), Recipe.REPLICATOR_FLUIDSTACK_RECIPE.getJEICategory());
    }

    private static <INPUT extends MachineInput<INPUT>, OUTPUT extends MachineOutput<OUTPUT>, RECIPE extends MachineRecipe<INPUT, OUTPUT, RECIPE>>
    void addRecipes(IModRegistry registry, Recipe<INPUT, OUTPUT, RECIPE> type, IRecipeWrapperFactory<RECIPE> factory) {
        String recipeCategoryUid = type.getJEICategory();
        registry.handleRecipes(type.getRecipeClass(), factory, recipeCategoryUid);
        registry.addRecipes(type.get().values().stream().map(factory::getRecipeWrapper).collect(Collectors.toList()), recipeCategoryUid);
    }

    private static void registerRecipeMachineTierItem(IModRegistry registry, Block block, String... recipe) {
        for (MachineTier tier : MachineTier.values()) {
            ItemStack add = new ItemStack(block);
            if (add.getItem() instanceof ITierItem tierItem) {
                tierItem.setBaseTier(add, tier.getBaseTier());
            }
            registry.addRecipeCatalyst(add, recipe);
        }
    }

    private static ItemStack getTierFactory(Block block, MachineTier tier) {
        return new ItemStack(block, 1, tier.ordinal());
    }
}
