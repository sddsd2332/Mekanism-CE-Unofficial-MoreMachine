package mekceumoremachine.common.registries;

import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.block.*;
import mekceumoremachine.common.item.itemBlock.*;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.registries.IForgeRegistry;

@ObjectHolder(MEKCeuMoreMachine.MODID)
public class MEKCeuMoreMachineBlocks {

    public static Block WirelessCharging = new BlockWirelessCharging();
    public static Block TierElectricPump = new BlockTierElectricPump();
    public static Block TierIsotopicCentrifuge = new BlockTierIsotopicCentrifuge();
    public static Block TierRotaryCondensentrator = new BlockTierRotaryCondensentrator();
    public static Block TierElectrolyticSeparator = new BlockTierElectrolyticSeparator();
    public static Block TierSolarNeutronActivator = new BlockTierSolarNeutronActivator();
    public static Block TierChemicalInfuser = new BlockTierChemicalInfuser();
    public static Block TierAmbientAccumulator = new BlockTierAmbientAccumulator();
    public static Block TierRadioactiveWasteBarrel = new BlockTierRadioactiveWasteBarrel();
    public static Block TierChemicalWasher = new BlockTierChemicalWasher();
    public static Block TierWindGenerator = new BlockTierWindGenerator();
    public static Block BigWindGenerator = new BlockBigWindGenerator();

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        registry.register(init(WirelessCharging, "WirelessCharging"));
        registry.register(init(TierElectricPump, "TierElectricPump"));
        registry.register(init(TierIsotopicCentrifuge, "TierIsotopicCentrifuge"));
        registry.register(init(TierRotaryCondensentrator, "TierRotaryCondensentrator"));
        registry.register(init(TierElectrolyticSeparator, "TierElectrolyticSeparator"));
        registry.register(init(TierSolarNeutronActivator, "TierSolarNeutronActivator"));
        registry.register(init(TierChemicalInfuser, "TierChemicalInfuser"));
        registry.register(init(TierAmbientAccumulator, "TierAmbientAccumulator"));
        registry.register(init(TierRadioactiveWasteBarrel, "TierRadioactiveWasteBarrel"));
        registry.register(init(TierChemicalWasher, "TierChemicalWasher"));
        registry.register(init(TierWindGenerator, "TierWindGenerator"));
        registry.register(init(BigWindGenerator,"BigWindGenerator"));
    }

    public static void registerItemBlocks(IForgeRegistry<Item> registry) {
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockWirelessCharging(WirelessCharging), "WirelessCharging"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierElectricPump(TierElectricPump), "TierElectricPump"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierIsotopicCentrifuge(TierIsotopicCentrifuge), "TierIsotopicCentrifuge"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierRotaryCondensentrator(TierRotaryCondensentrator), "TierRotaryCondensentrator"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierElectrolyticSeparator(TierElectrolyticSeparator), "TierElectrolyticSeparator"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierSolarNeutronActivator(TierSolarNeutronActivator), "TierSolarNeutronActivator"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierChemicalInfuser(TierChemicalInfuser), "TierChemicalInfuser"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierAmbientAccumulator(TierAmbientAccumulator), "TierAmbientAccumulator"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierRadioactiveWasteBarrel(TierRadioactiveWasteBarrel), "TierRadioactiveWasteBarrel"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierChemicalWasher(TierChemicalWasher), "TierChemicalWasher"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockTierWindGenerator(TierWindGenerator), "TierWindGenerator"));
        registry.register(MEKCeuMoreMachineItems.init(new ItemBlockBigWindGenerator(BigWindGenerator),"BigWindGenerator"));
    }

    public static Block init(Block block, String name) {
        return block.setTranslationKey(name).setRegistryName(new ResourceLocation(MEKCeuMoreMachine.MODID, name));
    }
}
