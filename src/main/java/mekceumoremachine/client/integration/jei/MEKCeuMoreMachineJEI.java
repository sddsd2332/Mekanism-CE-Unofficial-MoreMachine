package mekceumoremachine.client.integration.jei;


import mekanism.client.jei.GuiElementHandler;
import mekanism.common.base.ITierItem;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.util.LangUtils;
import mekceumoremachine.client.integration.jei.machine.other.ReplicatorFluidStackRecipeCategory;
import mekceumoremachine.client.integration.jei.machine.other.ReplicatorGasesRecipeCategory;
import mekceumoremachine.client.integration.jei.machine.other.ReplicatorItemStackRecipeCategory;
import mekceumoremachine.common.item.itemBlock.ItemBlockWirelessCharging;
import mekceumoremachine.common.item.itemBlock.ItemBlockWirelessEnergy;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.util.VoidMineralGeneratorUitls;
import mezz.jei.api.*;
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.Locale;
import java.util.stream.Collectors;

@JEIPlugin
public class MEKCeuMoreMachineJEI implements IModPlugin {

    public static final ISubtypeInterpreter NBT_INTERPRETER = itemStack -> {
        String ret = Integer.toString(itemStack.getMetadata());

        if (itemStack.getItem() instanceof ITierItem tierItem) {
            ret += ":" + tierItem.getBaseTier(itemStack).getSimpleName();
        }
        if (itemStack.getItem() instanceof ItemBlockWirelessCharging wireless) {
            ret += ":" + (wireless.getEnergy(itemStack) > 0 ? "filled" : "empty");
        }
        if (itemStack.getItem() instanceof ItemBlockWirelessEnergy wireless) {
            ret += ":" + (wireless.getEnergy(itemStack) > 0 ? "filled" : "empty");
        }

        return ret.toLowerCase(Locale.ROOT);
    };

    @Override
    public void registerSubtypes(ISubtypeRegistry registry) {
        registry(registry, MEKCeuMoreMachineBlocks.WirelessCharging);
        registry(registry, MEKCeuMoreMachineBlocks.TierElectricPump);
        registry(registry, MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge);
        registry(registry, MEKCeuMoreMachineBlocks.TierRotaryCondensentrator);
        registry(registry, MEKCeuMoreMachineBlocks.TierElectrolyticSeparator);
        registry(registry, MEKCeuMoreMachineBlocks.TierSolarNeutronActivator);
        registry(registry, MEKCeuMoreMachineBlocks.TierChemicalInfuser);
        registry(registry, MEKCeuMoreMachineBlocks.TierAmbientAccumulator);
        registry(registry, MEKCeuMoreMachineBlocks.TierChemicalWasher);
        registry(registry, MEKCeuMoreMachineBlocks.TierWindGenerator);
        registry(registry, MEKCeuMoreMachineBlocks.TierChemicalDissolutionChamber);
        registry(registry, MEKCeuMoreMachineBlocks.TierNutritionalLiquifier);
        registry(registry, MEKCeuMoreMachineBlocks.TierChemicalOxidizer);
        registry(registry, MEKCeuMoreMachineBlocks.TierGasGenerator);
        registry(registry, MEKCeuMoreMachineBlocks.ReplicatorItemStack);
        registry(registry, MEKCeuMoreMachineBlocks.ReplicatorGases);
        registry(registry, MEKCeuMoreMachineBlocks.ReplicatorFluidStack);
        registry(registry, MEKCeuMoreMachineBlocks.WirelessEnergy);
        registry(registry, MEKCeuMoreMachineBlocks.TierSolarGenerator);
        registry(registry, MEKCeuMoreMachineBlocks.TierAdvancedSolarGenerator);
        registry(registry, MEKCeuMoreMachineBlocks.VoidMineralGenerator);
    }


    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(new ReplicatorItemStackRecipeCategory<>(guiHelper));
        registry.addRecipeCategories(new ReplicatorGasesRecipeCategory<>(guiHelper));
        registry.addRecipeCategories(new ReplicatorFluidStackRecipeCategory<>(guiHelper));
    }

    public void registry(ISubtypeRegistry registry, Block block) {
        registry.registerSubtypeInterpreter(Item.getItemFromBlock(block), NBT_INTERPRETER);
    }

    @Override
    public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(new GuiElementHandler());
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierIsotopicCentrifuge(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierRotaryCondensentrator(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierElectrolyticSeparator(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierNeutronActivator(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierChemicalInfuser(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierAmbientAccumulator(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierWasher(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierChemicalDissolutionChamber(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierNutritionalLiquifier(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerTierChemicalOxidizer(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerGasStackFlueToEnergyRecipe(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerReplicatorItemStackRecipe(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerReplicatorGasesRecipe(registry);
        MEKCeuMoreMachineRecipeRegistryHelper.registerReplicatorFluidStackRecipe(registry);
        registry.addIngredientInfo(VoidMineralGeneratorUitls.getCanOre(), VanillaTypes.ITEM, LangUtils.localize("gui.canOre"));
    }
}
