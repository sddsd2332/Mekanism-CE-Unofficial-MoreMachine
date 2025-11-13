package mekceumoremachine.client.integration.jei;

import mekanism.common.base.ITierItem;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekceumoremachine.client.gui.*;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tier.MachineTier;
import mezz.jei.api.IModRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class MEKCeuMoreMachineRecipeRegistryHelper {

    public static void registerTierIsotopicCentrifuge(IModRegistry registry) {
        registry.addRecipeClickArea(GuiTierIsotopicCentrifuge.class, 61, 39, 55, 8, Recipe.ISOTOPIC_CENTRIFUGE.getJEICategory());
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge, Recipe.ISOTOPIC_CENTRIFUGE.getJEICategory());
    }

    public static void registerTierRotaryCondensentrator(IModRegistry registry) {
        String condensentrating = "mekanism.rotary_condensentrator_condensentrating";
        String decondensentrating = "mekanism.rotary_condensentrator_decondensentrating";
        registry.addRecipeClickArea(GuiTierRotaryCondensentrator.class, 64, 39, 48, 8, condensentrating, decondensentrating);
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierRotaryCondensentrator, condensentrating, decondensentrating);
    }

    public static void registerTierElectrolyticSeparator(IModRegistry registry) {
        registry.addRecipeClickArea(GuiTierElectrolyticSeparator.class, 80, 30, 16, 6, Recipe.ELECTROLYTIC_SEPARATOR.getJEICategory());
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierElectrolyticSeparator, Recipe.ELECTROLYTIC_SEPARATOR.getJEICategory());
    }

    public static void registerTierNeutronActivator(IModRegistry registry) {
        registry.addRecipeClickArea(GuiTierSolarNeutronActivator.class, 64, 39, 48, 8, Recipe.SOLAR_NEUTRON_ACTIVATOR.getJEICategory());
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierSolarNeutronActivator, Recipe.SOLAR_NEUTRON_ACTIVATOR.getJEICategory());
    }

    public static void registerTierChemicalInfuser(IModRegistry registry) {
        registry.addRecipeClickArea(GuiTierChemicalInfuser.class, 47, 39 + 11, 28, 8, Recipe.CHEMICAL_INFUSER.getJEICategory());
        registry.addRecipeClickArea(GuiTierChemicalInfuser.class, 101, 39 + 11, 28, 8, Recipe.CHEMICAL_INFUSER.getJEICategory());
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierChemicalInfuser, Recipe.CHEMICAL_INFUSER.getJEICategory());
    }

    public static void registerTierAmbientAccumulator(IModRegistry registry) {
        registry.addRecipeClickArea(GuiTierAmbientAccumulator.class, 7, 18, 80, 65, Recipe.AMBIENT_ACCUMULATOR.getJEICategory());
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierAmbientAccumulator, Recipe.AMBIENT_ACCUMULATOR.getJEICategory());
    }

    public static void registerTierWasher(IModRegistry registry) {
        registry.addRecipeClickArea(GuiTierChemicalWasher.class, 61, 39, 55, 8, Recipe.CHEMICAL_WASHER.getJEICategory());
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


    public static void registerGasStackFlueToEnergyRecipe(IModRegistry registry) {
        registry.addRecipeClickArea(GuiTierGasGenerator.class, 55, 18, 66, 50, RecipeHandler.Recipe.GAS_FUEL_TO_ENERGY_RECIPE.getJEICategory());
        registerRecipeMachineTierItem(registry, MEKCeuMoreMachineBlocks.TierGasGenerator, Recipe.GAS_FUEL_TO_ENERGY_RECIPE.getJEICategory());
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
