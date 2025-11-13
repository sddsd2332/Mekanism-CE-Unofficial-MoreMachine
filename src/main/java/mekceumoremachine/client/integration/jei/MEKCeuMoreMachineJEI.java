package mekceumoremachine.client.integration.jei;


import mekanism.client.jei.GuiElementHandler;
import mekanism.common.base.ITierItem;
import mekceumoremachine.common.item.itemBlock.ItemBlockWirelessCharging;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter;
import mezz.jei.api.JEIPlugin;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.Locale;

@JEIPlugin
public class MEKCeuMoreMachineJEI implements IModPlugin {

    public static final ISubtypeInterpreter NBT_INTERPRETER = itemStack -> {
        String ret = Integer.toString(itemStack.getMetadata());

        if (itemStack.getItem() instanceof ITierItem tierItem) {
            ret += ":" + tierItem.getBaseTier(itemStack).getSimpleName();
        }
        if (itemStack.getItem() instanceof ItemBlockWirelessCharging wirelessCharging) {
            ret += ":" + (wirelessCharging.getEnergy(itemStack) > 0 ? "filled" : "empty");
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
        registry(registry,MEKCeuMoreMachineBlocks.TierChemicalOxidizer);
        registry(registry,MEKCeuMoreMachineBlocks.TierGasGenerator);
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
    }
}
