package mekceumoremachine.common.processing;

import mekanism.api.processing.MachinePort;
import mekanism.api.processing.MachinePresentationDescriptor;
import mekanism.api.processing.MachineRecipeProvider;
import mekanism.api.processing.MachineRecipeProviderRegistry;
import mekanism.api.processing.MachineRecipeRoute;
import mekanism.api.processing.MachineResourceStack;
import mekanism.api.processing.ProviderConformanceDescriptor;
import mekanism.api.processing.QIOAutomationMode;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.MekanismFluids;
import mekanism.common.base.IBaseTierProvider;
import mekanism.common.base.ITierItem;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.processing.MachineRecipeRouteCollectors;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.tile.machine.TileEntityTierAmbientAccumulator;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalInfuser;
import mekceumoremachine.common.tile.machine.TileEntityTierChemicalWasher;
import mekceumoremachine.common.tile.machine.TileEntityTierElectricPump;
import mekceumoremachine.common.tile.machine.TileEntityTierElectrolyticSeparator;
import mekceumoremachine.common.tile.machine.TileEntityTierIsotopicCentrifuge;
import mekceumoremachine.common.tile.machine.TileEntityTierRotaryCondensentrator;
import mekceumoremachine.common.tile.machine.TileEntityTierSolarNeutronActivator;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import mekceumoremachine.common.tile.machine.TierCrystallizer.TileEntityTierChemicalCrystallizer;
import mekceumoremachine.common.tile.machine.TierDissolution.TileEntityTierChemicalDissolutionChamber;
import mekceumoremachine.common.tile.machine.TierNutritional.TileEntityTierNutritionalLiquifier;
import mekceumoremachine.common.tile.machine.TierOxidizer.TileEntityTierChemicalOxidizer;
import mekceumoremachine.common.tile.machine.TierOrganicFarm.TileEntityTierOrganicFarm;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorFluidStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Registers MoreMachine processing routes and physical ports with Mekanism's shared bridge. */
public final class MoreMachineRecipeProviders {

    private static boolean registered;
    private static final ProviderConformanceDescriptor PROCESSING_CONFORMANCE =
          ProviderConformanceDescriptor.builder(MEKCeuMoreMachine.MODID, "processing")
                .supports(QIOAutomationMode.SCHEDULED, QIOAutomationMode.PASSIVE, QIOAutomationMode.OUTPUT_ONLY)
                .build();
    private static final ProviderConformanceDescriptor OUTPUT_CONFORMANCE =
          ProviderConformanceDescriptor.builder(MEKCeuMoreMachine.MODID, "output")
                .supports(QIOAutomationMode.OUTPUT_ONLY)
                .build();

    private MoreMachineRecipeProviders() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        register("tier_chemical_infuser", TileEntityTierChemicalInfuser.class, TileEntityTierChemicalInfuser::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectChemicalPairToGas(tile.getRecipes()),
              tile -> ports(
                    MachinePort.gas("left_gas", MachinePort.Role.INPUT, tile.leftTank),
                    MachinePort.gas("right_gas", MachinePort.Role.INPUT, tile.rightTank),
                    MachinePort.gas("gas_output", MachinePort.Role.OUTPUT, tile.centerTank)));
        register("tier_chemical_washer", TileEntityTierChemicalWasher.class, TileEntityTierChemicalWasher::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectGasFluidToGas(tile.getRecipes()),
              tile -> ports(
                    MachinePort.gas("gas_input", MachinePort.Role.INPUT, tile.inputTank),
                    MachinePort.fluid("fluid_input", MachinePort.Role.INPUT, tile.fluidTank),
                    MachinePort.gas("gas_output", MachinePort.Role.OUTPUT, tile.outputTank)));
        register("tier_electrolytic_separator", TileEntityTierElectrolyticSeparator.class,
              TileEntityTierElectrolyticSeparator::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectFluidToGasPair(tile.getRecipes()),
              tile -> ports(
                    MachinePort.fluid("fluid_input", MachinePort.Role.INPUT, tile.fluidTank),
                    MachinePort.gas("left_gas_output", MachinePort.Role.OUTPUT, tile.leftTank),
                    MachinePort.gas("right_gas_output", MachinePort.Role.OUTPUT, tile.rightTank)));
        register("tier_isotopic_centrifuge", TileEntityTierIsotopicCentrifuge.class,
              TileEntityTierIsotopicCentrifuge::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectGasToGas(tile.getRecipes()),
              tile -> ports(
                    MachinePort.gas("gas_input", MachinePort.Role.INPUT, tile.inputTank),
                    MachinePort.gas("gas_output", MachinePort.Role.OUTPUT, tile.outputTank)));
        register("tier_rotary_condensentrator", TileEntityTierRotaryCondensentrator.class,
              ignored -> RecipeHandler.Recipe.ROTARY_CONDENSENTRATOR.get(),
              tile -> tile.mode ?
                    MachineRecipeRouteCollectors.collectRotaryGasToFluid(RecipeHandler.Recipe.ROTARY_CONDENSENTRATOR.get()) :
                    MachineRecipeRouteCollectors.collectRotaryFluidToGas(RecipeHandler.Recipe.ROTARY_CONDENSENTRATOR.get()),
              tile -> tile.mode ? ports(
                    MachinePort.gas("gas_input", MachinePort.Role.INPUT, tile.gasTank),
                    MachinePort.fluid("fluid_output", MachinePort.Role.OUTPUT, tile.fluidTank)) : ports(
                    MachinePort.fluid("fluid_input", MachinePort.Role.INPUT, tile.fluidTank),
                    MachinePort.gas("gas_output", MachinePort.Role.OUTPUT, tile.gasTank)),
              tile -> tile.mode ? 1 : 0);
        register("tier_solar_neutron_activator", TileEntityTierSolarNeutronActivator.class,
              TileEntityTierSolarNeutronActivator::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectGasToGas(tile.getRecipes()),
              tile -> ports(
                    MachinePort.gas("gas_input", MachinePort.Role.INPUT, tile.inputTank),
                    MachinePort.gas("gas_output", MachinePort.Role.OUTPUT, tile.outputTank)));

        register("tier_chemical_crystallizer", TileEntityTierChemicalCrystallizer.class,
              ignored -> RecipeHandler.Recipe.CHEMICAL_CRYSTALLIZER.get(),
              tile -> MachineRecipeRouteCollectors.expandLanes(MachineRecipeRouteCollectors.collectGasToItem(
                    RecipeHandler.Recipe.CHEMICAL_CRYSTALLIZER.get()), tile.getGasSorterProcessCount()),
              MoreMachineRecipeProviders::crystallizerPorts);
        register("tier_chemical_oxidizer", TileEntityTierChemicalOxidizer.class,
              ignored -> RecipeHandler.Recipe.CHEMICAL_OXIDIZER.get(),
              tile -> MachineRecipeRouteCollectors.expandLanes(MachineRecipeRouteCollectors.collectItemToGas(
                    RecipeHandler.Recipe.CHEMICAL_OXIDIZER.get()), tile.getSorterProcessCount()),
              MoreMachineRecipeProviders::oxidizerPorts);
        register("tier_nutritional_liquifier", TileEntityTierNutritionalLiquifier.class,
              ignored -> RecipeHandler.Recipe.NUTRITIONAL_LIQUIFIER.get(),
              tile -> MachineRecipeRouteCollectors.expandLanes(MachineRecipeRouteCollectors.collectItemToGas(
                    RecipeHandler.Recipe.NUTRITIONAL_LIQUIFIER.get()), tile.getSorterProcessCount()),
              MoreMachineRecipeProviders::nutritionalPorts);
        register("tier_chemical_dissolution_chamber", TileEntityTierChemicalDissolutionChamber.class,
              ignored -> RecipeHandler.Recipe.CHEMICAL_DISSOLUTION_CHAMBER.get(),
              tile -> MachineRecipeRouteCollectors.expandLanes(MachineRecipeRouteCollectors.collectItemGasToGas(
                    RecipeHandler.Recipe.CHEMICAL_DISSOLUTION_CHAMBER.get(), MekanismFluids.SulfuricAcid,
                    tile.getGasUsagePerOperation()), tile.getSorterProcessCount(), "gas_input"),
              MoreMachineRecipeProviders::dissolutionPorts, TileEntityTierChemicalDissolutionChamber::getGasUsagePerOperation);
        register("tier_organic_farm", TileEntityTierOrganicFarm.class,
              TileEntityTierOrganicFarm::getRecipes,
              tile -> MachineRecipeRouteCollectors.expandLanes(
                    MachineRecipeRouteCollectors.collectFarmGasToItem(tile.getRecipes(), tile.getRecipeGasUsagePerOperation()),
                    tile.threadCount, "gas_input", "fluid_input", "item_output"),
              MoreMachineRecipeProviders::organicFarmPorts, TileEntityTierOrganicFarm::getRecipeGasUsagePerOperation);

        registerOutput("tier_ambient_accumulator", TileEntityTierAmbientAccumulator.class, ignored -> null,
              ignored -> Collections.emptyList(),
              tile -> ports(MachinePort.gas("gas_output", MachinePort.Role.OUTPUT, tile.outputTank)));
        registerOutput("tier_electric_pump", TileEntityTierElectricPump.class, ignored -> null,
              ignored -> Collections.emptyList(),
              tile -> ports(MachinePort.fluid("fluid_output", MachinePort.Role.OUTPUT, tile.fluidTank)));

        register("replicator_item", TileEntityReplicatorItemStack.class, TileEntityReplicatorItemStack::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectConfigurableReplicatorItems(tile.getRecipes()),
              tile -> ports(
                    MachinePort.configurationItem("template", tile.getTemplateSlot(), "template", 0),
                    MachinePort.gas("uu_input", MachinePort.Role.INPUT, tile.inputGasTank),
                    MachinePort.item("item_output", MachinePort.Role.OUTPUT, tile.getRecipeOutputSlot())),
              tile -> resourceRevision(tile.getTemplateSlot().isEmpty() ? null :
                    MachineResourceStack.item("template", tile.getTemplateSlot().getStack())));
        register("replicator_gas", TileEntityReplicatorGases.class, TileEntityReplicatorGases::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectConfigurableReplicatorGases(tile.getRecipes()),
              tile -> ports(
                    MachinePort.configurationGas("template", tile.inputTank, "template", 0),
                    MachinePort.gas("uu_input", MachinePort.Role.INPUT, tile.uuTank),
                    MachinePort.gas("gas_output", MachinePort.Role.OUTPUT, tile.outputTank)),
              tile -> resourceRevision(tile.inputTank.getGas() == null ? null :
                    MachineResourceStack.gas("template", tile.inputTank.getGas())));
        register("replicator_fluid", TileEntityReplicatorFluidStack.class, TileEntityReplicatorFluidStack::getRecipes,
              tile -> MachineRecipeRouteCollectors.collectConfigurableReplicatorFluids(tile.getRecipes()),
              tile -> ports(
                    MachinePort.configurationFluid("template", tile.inputTank, "template", 0),
                    MachinePort.gas("uu_input", MachinePort.Role.INPUT, tile.uuTank),
                    MachinePort.fluid("fluid_output", MachinePort.Role.OUTPUT, tile.outputTank)),
              tile -> resourceRevision(tile.inputTank.getFluid() == null ? null :
                    MachineResourceStack.fluid("template", tile.inputTank.getFluid())));
        registerOutput("void_mineral_generator", TileEntityVoidMineralGenerator.class, ignored -> null,
              ignored -> Collections.emptyList(), MoreMachineRecipeProviders::voidMineralGeneratorPorts);
        registered = true;
    }

    private static List<MachinePort> crystallizerPorts(TileEntityTierChemicalCrystallizer tile) {
        List<MachinePort> ports = new ArrayList<>();
        for (int process = 0; process < tile.getGasSorterProcessCount(); process++) {
            add(ports, MachinePort.gas("gas_input_" + process, MachinePort.Role.INPUT,
                  tile.getGasSorterInputTank(process), "gas_input", process));
            add(ports, MachinePort.item("item_output_" + process, MachinePort.Role.OUTPUT,
                  tile.getProcessingOutputSlot(process), "item_output", process));
        }
        return ports;
    }

    private static List<MachinePort> oxidizerPorts(TileEntityTierChemicalOxidizer tile) {
        List<MachinePort> ports = new ArrayList<>();
        for (int process = 0; process < tile.getSorterProcessCount(); process++) {
            add(ports, MachinePort.item("item_input_" + process, MachinePort.Role.INPUT,
                  tile.getSorterInputSlot(process), "item_input", process));
            add(ports, MachinePort.gas("gas_output_" + process, MachinePort.Role.OUTPUT,
                  tile.outPutTanks[process], "gas_output", process));
        }
        return ports;
    }

    private static List<MachinePort> nutritionalPorts(TileEntityTierNutritionalLiquifier tile) {
        List<MachinePort> ports = new ArrayList<>();
        for (int process = 0; process < tile.getSorterProcessCount(); process++) {
            add(ports, MachinePort.item("item_input_" + process, MachinePort.Role.INPUT,
                  tile.getSorterInputSlot(process), "item_input", process));
            add(ports, MachinePort.gas("gas_output_" + process, MachinePort.Role.OUTPUT,
                  tile.outPutTanks[process], "gas_output", process));
        }
        return ports;
    }

    private static List<MachinePort> dissolutionPorts(TileEntityTierChemicalDissolutionChamber tile) {
        List<MachinePort> ports = new ArrayList<>();
        add(ports, MachinePort.gas("gas_input", MachinePort.Role.INPUT, tile.injectTank,
              "gas_input", MachinePort.SHARED_LANE));
        for (int process = 0; process < tile.getSorterProcessCount(); process++) {
            add(ports, MachinePort.item("item_input_" + process, MachinePort.Role.INPUT,
                  tile.getSorterInputSlot(process), "item_input", process));
            add(ports, MachinePort.gas("gas_output_" + process, MachinePort.Role.OUTPUT,
                  tile.outPutTanks[process], "gas_output", process));
        }
        return ports;
    }

    private static List<MachinePort> voidMineralGeneratorPorts(TileEntityVoidMineralGenerator tile) {
        List<MachinePort> ports = new ArrayList<>();
        List<IInventorySlot> slots = tile.getOutputSlots();
        for (int index = 0; index < slots.size(); index++) {
            add(ports, MachinePort.item("item_output_" + index, MachinePort.Role.OUTPUT, slots.get(index)));
        }
        return ports;
    }

    private static List<MachinePort> organicFarmPorts(TileEntityTierOrganicFarm tile) {
        List<MachinePort> ports = new ArrayList<>();
        for (int lane = 0; lane < tile.threadCount; lane++) {
            add(ports, MachinePort.item("item_input_" + lane, MachinePort.Role.INPUT,
                  tile.inputSlots[lane], "item_input", lane));
        }
        add(ports, MachinePort.gas("gas_input", MachinePort.Role.INPUT, tile.mergedTank.getGasTank(),
              "gas_input", MachinePort.SHARED_LANE));
        add(ports, MachinePort.fluid("fluid_input", MachinePort.Role.INPUT, tile.mergedTank.getFluidTank(),
              "fluid_input", MachinePort.SHARED_LANE));
        add(ports, MachinePort.itemGroup("item_output", MachinePort.Role.OUTPUT, tile.outputSlots,
              "item_output", MachinePort.SHARED_LANE));
        return ports;
    }

    private static int resourceRevision(MachineResourceStack stack) {
        return stack == null ? 0 : stack.hashCode();
    }

    private static MachinePresentationDescriptor presentation(TileEntity tile) {
        MachinePresentationDescriptor fallback = MachinePresentationDescriptor.fallback(tile);
        if (!(tile instanceof IBaseTierProvider tierProvider) || tierProvider.getBaseTier() == null) {
            return fallback;
        }
        Block block = tile.getBlockType();
        Item item = block == null ? null : Item.getItemFromBlock(block);
        if (!(item instanceof ITierItem tierItem)) {
            return fallback;
        }
        ItemStack stack = new ItemStack(item, 1, fallback.getItemMetadata());
        tierItem.setBaseTier(stack, tierProvider.getBaseTier());
        return MachinePresentationDescriptor.fromStack(stack);
    }

    private static List<MachinePort> ports(MachinePort... values) {
        List<MachinePort> ports = new ArrayList<>(values.length);
        for (MachinePort value : values) {
            add(ports, value);
        }
        return ports;
    }

    private static void add(List<MachinePort> ports, MachinePort port) {
        if (port != null) {
            ports.add(port);
        }
    }

    private static <TILE extends TileEntity> void register(String path, Class<TILE> tileClass,
          Function<TILE, Object> recipeSource, Function<TILE, List<MachineRecipeRoute>> routes,
          Function<TILE, List<MachinePort>> ports) {
        register(path, tileClass, recipeSource, routes, ports, ignored -> 0, PROCESSING_CONFORMANCE);
    }

    private static <TILE extends TileEntity> void register(String path, Class<TILE> tileClass,
          Function<TILE, Object> recipeSource, Function<TILE, List<MachineRecipeRoute>> routes,
          Function<TILE, List<MachinePort>> ports, ToIntFunction<TILE> additionalRevision) {
        register(path, tileClass, recipeSource, routes, ports, additionalRevision, PROCESSING_CONFORMANCE);
    }

    private static <TILE extends TileEntity> void registerOutput(String path, Class<TILE> tileClass,
          Function<TILE, Object> recipeSource, Function<TILE, List<MachineRecipeRoute>> routes,
          Function<TILE, List<MachinePort>> ports) {
        register(path, tileClass, recipeSource, routes, ports, ignored -> 0, OUTPUT_CONFORMANCE);
    }

    private static <TILE extends TileEntity> void register(String path, Class<TILE> tileClass,
          Function<TILE, Object> recipeSource, Function<TILE, List<MachineRecipeRoute>> routes,
          Function<TILE, List<MachinePort>> ports, ToIntFunction<TILE> additionalRevision,
          ProviderConformanceDescriptor conformance) {
        MachineRecipeProviderRegistry.register(new ResourceLocation(MEKCeuMoreMachine.MODID, path), tileClass,
              new MachineRecipeProvider<TILE>() {
                  @Override
                  public Object getRecipeSourceKey(TILE tile) {
                      return recipeSource.apply(tile);
                  }

                  @Override
                  public int getConfigurationRevision(TILE tile) {
                      int revision = RecipeHandler.getGlobalRecipeVersion();
                      revision = 31 * revision + (tile instanceof IBaseTierProvider tierProvider ? tierProvider.getBaseTier().ordinal() : 0);
                      return 31 * revision + additionalRevision.applyAsInt(tile);
                  }

                  @Override
                  public List<MachineRecipeRoute> getRecipeRoutes(TILE tile) {
                      return routes.apply(tile);
                  }

                  @Override
                  public List<MachinePort> getPorts(TILE tile) {
                      return ports.apply(tile);
                  }

                  @Override
                  public MachinePresentationDescriptor getPresentation(TILE tile) {
                      return presentation(tile);
                  }

                  @Override
                  public ProviderConformanceDescriptor getQIOConformance(TILE tile) {
                      return conformance;
                  }
              });
    }
}
