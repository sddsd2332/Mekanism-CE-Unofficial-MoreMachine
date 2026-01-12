package mekceumoremachine.common;

import mekanism.common.base.IGuiProvider;
import mekceumoremachine.common.inventory.container.*;
import mekceumoremachine.common.tile.generator.TileEntityBaseWindGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;
import mekceumoremachine.common.tile.machine.TierDissolution.*;
import mekceumoremachine.common.tile.machine.TierNutritional.*;
import mekceumoremachine.common.tile.machine.TierOxidizer.*;
import mekceumoremachine.common.tile.machine.*;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorFluidStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CommonProxy implements IGuiProvider {

    private static void registerTileEntity(Class<? extends TileEntity> clazz, String name) {
        GameRegistry.registerTileEntity(clazz, new ResourceLocation(MEKCeuMoreMachine.MODID, name));
    }


    public void registerTileEntities() {
        registerTileEntity(TileEntityWirelessChargingStation.class, "wireless_charging_station");
        registerTileEntity(TileEntityTierElectricPump.class, "tier_electric_pump");
        registerTileEntity(TileEntityTierIsotopicCentrifuge.class, "tier_isotopic_centrifuge");
        registerTileEntity(TileEntityTierRotaryCondensentrator.class, "tier_rotary_condensentrator");
        registerTileEntity(TileEntityTierElectrolyticSeparator.class, "tier_electrolytic_separator");
        registerTileEntity(TileEntityTierSolarNeutronActivator.class, "tier_solar_neutron_activator");
        registerTileEntity(TileEntityTierChemicalInfuser.class, "tier_chemical_infuser");
        registerTileEntity(TileEntityTierAmbientAccumulator.class, "tier_ambient_accumulator");
        registerTileEntity(TileEntityTierRadioactiveWasteBarrel.class, "tier_radioactive_waste_barrel");
        registerTileEntity(TileEntityTierChemicalWasher.class, "tier_chemical_washer");
        registerTileEntity(TileEntityTierWindGenerator.class, "tier_wind_generator");
        registerTileEntity(TileEntityTierChemicalDissolutionChamberBasic.class, "tier_chemical_dissolution_chamber_basic");
        registerTileEntity(TileEntityTierChemicalDissolutionChamberAdvanced.class, "tier_chemical_dissolution_chamber_advanced");
        registerTileEntity(TileEntityTierChemicalDissolutionChamberElite.class, "tier_chemical_dissolution_chamber_elite");
        registerTileEntity(TileEntityTierChemicalDissolutionChamberUltimate.class, "tier_chemical_dissolution_chamber_ultimate");
        registerTileEntity(TileEntityTierNutritionalLiquifierBasic.class, "tier_nutritional_liquifier_basic");
        registerTileEntity(TileEntityTierNutritionalLiquifierAdvanced.class, "tier_nutritional_liquifier_advanced");
        registerTileEntity(TileEntityTierNutritionalLiquifierElite.class, "tier_nutritional_liquifier_elite");
        registerTileEntity(TileEntityTierNutritionalLiquifierUltimate.class, "tier_nutritional_liquifier_ultimate");
        registerTileEntity(TileEntityTierChemicalOxidizerBasic.class, "tier_chemical_oxidizer_basic");
        registerTileEntity(TileEntityTierChemicalOxidizerAdvanced.class, "tier_chemical_oxidizer_advanced");
        registerTileEntity(TileEntityTierChemicalOxidizerElite.class, "tier_chemical_oxidizer_elite");
        registerTileEntity(TileEntityTierChemicalOxidizerUltimate.class, "tier_chemical_oxidizer_ultimate");
        registerTileEntity(TileEntityTierGasGenerator.class,"tier_gas_generator");
        registerTileEntity(TileEntityReplicatorItemStack.class,"replicator_itemstack");
        registerTileEntity(TileEntityReplicatorGases.class,"replicator_gases");
        registerTileEntity(TileEntityReplicatorFluidStack.class,"replicator_fluidstack");
        registerTileEntity(TileEntityWirelessChargingEnergy.class,"wireless_charging_energy");
    }


    public void registerTESRs() {
    }

    public void registerItemRenders() {
    }

    public void registerBlockRenders() {
    }

    public void init() {
    }

    public void preInit() {
    }



    @Override
    public Object getClientGui(int ID, EntityPlayer player, World world, BlockPos pos) {
        return null;
    }


    @Override
    public Container getServerGui(int ID, EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return switch (ID) {
            case 0 -> new ContainerWirelessCharging(player.inventory, (TileEntityWirelessChargingStation) tileEntity);
            case 1 -> new ContainerTierElectricPump(player.inventory, (TileEntityTierElectricPump) tileEntity);
            case 2 -> new ContainerTierIsotopicCentrifuge(player.inventory, (TileEntityTierIsotopicCentrifuge) tileEntity);
            case 3 -> new ContainerTierRotaryCondensentrator(player.inventory, (TileEntityTierRotaryCondensentrator) tileEntity);
            case 4 -> new ContainerTierElectrolyticSeparator(player.inventory, (TileEntityTierElectrolyticSeparator) tileEntity);
            case 5 -> new ContainerTierSolarNeutronActivator(player.inventory, (TileEntityTierSolarNeutronActivator) tileEntity);
            case 6 -> new ContainerTierChemicalInfuser(player.inventory, (TileEntityTierChemicalInfuser) tileEntity);
            case 7 -> new ContainerTierAmbientAccumulator(player.inventory, (TileEntityTierAmbientAccumulator) tileEntity);
            case 8 -> new ContainerTierChemicalWasher(player.inventory, (TileEntityTierChemicalWasher) tileEntity);
            case 9 -> new ContainerBaseWindGenerator(player.inventory, (TileEntityBaseWindGenerator) tileEntity);
            case 10 -> new ContainerTierChemicalDissolutionChamber(player.inventory, (TileEntityTierChemicalDissolutionChamber) tileEntity);
            case 11 -> new ContainerTierNutritionalLiquifier(player.inventory, (TileEntityTierNutritionalLiquifier) tileEntity);
            case 12 -> new ContainerTierChemicalOxidizer(player.inventory, (TileEntityTierChemicalOxidizer) tileEntity);
            case 13 -> new ContainerTierGasGenerator(player.inventory, (TileEntityTierGasGenerator) tileEntity);
            case 14 -> new ContainerReplicatorItemStack(player.inventory,(TileEntityReplicatorItemStack) tileEntity);
            case 15 -> new ContainerReplicatorGases(player.inventory,(TileEntityReplicatorGases) tileEntity);
            case 16 -> new ContainerReplicatorFluidStack(player.inventory,(TileEntityReplicatorFluidStack) tileEntity);
            case 17 -> new ContainerWirelessEnergy(player.inventory,(TileEntityWirelessChargingEnergy) tileEntity);
            default -> null;
        };
    }
}
