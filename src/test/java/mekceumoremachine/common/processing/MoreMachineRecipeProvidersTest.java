package mekceumoremachine.common.processing;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.processing.MachinePort;
import mekanism.api.processing.MachinePresentationDescriptor;
import mekanism.api.processing.MachineRecipeProvider;
import mekanism.api.processing.MachineRecipeProviderRegistry;
import mekanism.api.processing.MachineRecipeRoute;
import mekanism.api.processing.MachineResourceKind;
import mekanism.api.processing.MachineResourceStack;
import mekanism.api.processing.QIOAutomationMode;
import mekanism.common.MekanismFluids;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.base.ITierItem;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.recipe.machines.ReplicatorFluidStackRecipe;
import mekanism.common.recipe.machines.ReplicatorGasStackRecipe;
import mekanism.common.recipe.machines.ReplicatorItemStackRecipe;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import mekanism.qioprocessing.common.QIOProcessingUpgrades;
import mekanism.qioprocessing.common.content.QIOAutomationUpgradeSupport;
import mekanism.qioprocessing.common.machine.QIOAutomationCapabilities;
import mekanism.qioprocessing.common.machine.QIOAutomationEventHandler;
import mekceumoremachine.common.block.BlockTierRotaryCondensentrator;
import mekceumoremachine.common.item.itemBlock.ItemBlockTierRotaryCondensentrator;
import mekceumoremachine.common.recipe.cache.inputs.TemplateInputHelper;
import mekceumoremachine.common.tier.MachineTier;
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
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorFluidStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.registries.GameData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoreMachineRecipeProvidersTest {

    private static final ResourceLocation UNREGISTERED_PROVIDER =
          new ResourceLocation("mekceumoremachine", "test_unregistered");
    private static Gas inputGas;
    private static Gas secondaryGas;
    private static Gas outputGas;
    private static Gas uuGas;
    private static Fluid inputFluid;
    private static Block presentationBlock;
    private static ItemBlockTierRotaryCondensentrator presentationItem;

    @BeforeAll
    static void registerProvidersAndRecipes() throws ReflectiveOperationException {
        Loader loader = Loader.instance();
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        if (namedMods.get(loader) == null) {
            namedMods.set(loader, Collections.emptyMap());
        }
        Bootstrap.register();
        inputGas = gas("more_machine_qio_input", 0x315F76);
        secondaryGas = gas("more_machine_qio_secondary", 0x8F5F40);
        outputGas = gas("more_machine_qio_output", 0x5F8F40);
        uuGas = gas("more_machine_qio_uu", 0x7F4F9F);
        inputFluid = fluid("more_machine_qio_fluid");
        presentationBlock = new BlockTierRotaryCondensentrator()
              .setRegistryName("mekceumoremachine", "test_qio_presentation_rotary");
        presentationItem = (ItemBlockTierRotaryCondensentrator) new ItemBlockTierRotaryCondensentrator(presentationBlock)
              .setRegistryName("mekceumoremachine", "test_qio_presentation_rotary");
        GameData.getBlockItemMap().put(presentationBlock, presentationItem);
        addTestRecipes();
        MoreMachineRecipeProviders.register();
    }

    @Test
    void everyProviderDeclaresItsStableOwnerAndSupportedModes() {
        for (Map.Entry<String, TileEntity> entry : processingTiles().entrySet()) {
            MachineRecipeProviderRegistry.BoundProvider provider = provider(entry.getValue());
            assertEquals(id(entry.getKey()), provider.id());
            assertEquals("mekceumoremachine", provider.getQIOConformance().ownerModId());
            assertEquals("processing", provider.getQIOConformance().stableProviderSlotId());
            assertTrue(provider.getQIOConformance().supports(QIOAutomationMode.SCHEDULED));
            assertTrue(provider.getQIOConformance().supports(QIOAutomationMode.PASSIVE));
            assertTrue(provider.getQIOConformance().supports(QIOAutomationMode.OUTPUT_ONLY));
        }
        for (Map.Entry<String, TileEntity> entry : outputTiles().entrySet()) {
            MachineRecipeProviderRegistry.BoundProvider provider = provider(entry.getValue());
            assertEquals(id(entry.getKey()), provider.id());
            assertEquals("mekceumoremachine", provider.getQIOConformance().ownerModId());
            assertEquals("output", provider.getQIOConformance().stableProviderSlotId());
            assertEquals(Collections.singleton(QIOAutomationMode.OUTPUT_ONLY),
                  provider.getQIOConformance().modes());
        }
    }

    @Test
    void everyDeclaredModePassesStructuralConformance() {
        for (TileEntity tile : processingTiles().values()) {
            MachineRecipeProviderRegistry.BoundProvider provider = provider(tile);
            for (QIOAutomationMode mode : QIOAutomationMode.values()) {
                assertTrue(provider.validateQIOConformance(mode).isConformant(),
                      provider.id() + " rejected " + mode + ": " + provider.validateQIOConformance(mode).errors());
            }
        }
        for (TileEntity tile : outputTiles().values()) {
            MachineRecipeProviderRegistry.BoundProvider provider = provider(tile);
            assertTrue(provider.validateQIOConformance(QIOAutomationMode.OUTPUT_ONLY).isConformant());
            assertFalse(provider.validateQIOConformance(QIOAutomationMode.SCHEDULED).isConformant());
            assertFalse(provider.validateQIOConformance(QIOAutomationMode.PASSIVE).isConformant());
        }
    }

    @Test
    void multiLaneRoutesUseOnlyTheirPhysicalLane() {
        assertLaneLayout(provider(configure(new TileEntityTierChemicalCrystallizer(MachineTier.BASIC))), 3, false);
        assertLaneLayout(provider(configure(new TileEntityTierChemicalOxidizer(MachineTier.BASIC))), 3, false);
        assertLaneLayout(provider(configure(new TileEntityTierNutritionalLiquifier(MachineTier.BASIC))), 3, false);
        assertLaneLayout(provider(configure(new TileEntityTierChemicalDissolutionChamber(MachineTier.BASIC))), 3, true);
    }

    @Test
    void templateReplicatorsExposeConfigurationAndConsumeOnlyUUGas() {
        TileEntityReplicatorItemStack item = configure(new TileEntityReplicatorItemStack());
        TileEntityReplicatorGases gas = configure(new TileEntityReplicatorGases());
        TileEntityReplicatorFluidStack fluid = configure(new TileEntityReplicatorFluidStack());

        assertTemplateProvider(provider(item), "replicator_item", "item_output", MachineResourceKind.ITEM);
        assertTemplateProvider(provider(gas), "replicator_gas", "gas_output", MachineResourceKind.GAS);
        assertTemplateProvider(provider(fluid), "replicator_fluid", "fluid_output", MachineResourceKind.FLUID);

        ItemStack itemBefore = item.getTemplateSlot().getStack().copy();
        GasStack gasBefore = gas.inputTank.getGas().copy();
        FluidStack fluidBefore = fluid.inputTank.getFluid().copy();
        TemplateInputHelper.getItemTemplateInputHandler(item.getTemplateSlot(), RecipeError.NOT_ENOUGH_INPUT)
              .use(itemBefore, 64);
        TemplateInputHelper.getGasTemplateInputHandler(gas.inputTank, RecipeError.NOT_ENOUGH_INPUT)
              .use(gasBefore, 64);
        TemplateInputHelper.getFluidTemplateInputHandler(fluid.inputTank, RecipeError.NOT_ENOUGH_INPUT)
              .use(fluidBefore, 64);
        assertTrue(ItemStack.areItemStacksEqual(itemBefore, item.getTemplateSlot().getStack()));
        assertEquals(gasBefore.amount, gas.inputTank.getStored());
        assertTrue(gasBefore.isGasEqual(gas.inputTank.getGas()));
        assertEquals(fluidBefore.amount, fluid.inputTank.getFluidAmount());
        assertTrue(fluidBefore.isFluidEqual(fluid.inputTank.getFluid()));
    }

    @Test
    void emptyTemplateReplicatorsRemainBindableAndPublishEveryConfigurableRoute() {
        TileEntityReplicatorItemStack item = new TileEntityReplicatorItemStack();
        TileEntityReplicatorGases gas = new TileEntityReplicatorGases();
        TileEntityReplicatorFluidStack fluid = new TileEntityReplicatorFluidStack();

        assertTemplateProvider(provider(item), "replicator_item", "item_output", MachineResourceKind.ITEM);
        assertTemplateProvider(provider(gas), "replicator_gas", "gas_output", MachineResourceKind.GAS);
        assertTemplateProvider(provider(fluid), "replicator_fluid", "fluid_output", MachineResourceKind.FLUID);
        assertTrue(provider(item).validateQIOEndpointConformance(QIOAutomationMode.SCHEDULED).isConformant());
        assertTrue(provider(gas).validateQIOEndpointConformance(QIOAutomationMode.PASSIVE).isConformant());
        assertTrue(provider(fluid).validateQIOEndpointConformance(QIOAutomationMode.SCHEDULED).isConformant());
        assertTrue(provider(item).validateQIOConformance(QIOAutomationMode.SCHEDULED).isConformant());
        assertTrue(provider(gas).validateQIOConformance(QIOAutomationMode.PASSIVE).isConformant());
        assertTrue(provider(fluid).validateQIOConformance(QIOAutomationMode.SCHEDULED).isConformant());
    }

    @Test
    void oneUnitTemplatesRunWithoutBeingConsumedOrLimitingBatchSize() {
        TileEntityReplicatorItemStack item = new TileEntityReplicatorItemStack();
        item.getTemplateSlot().setStack(new ItemStack(Items.IRON_INGOT));
        item.inputGasTank.setGas(new GasStack(uuGas, 5));
        ReplicatorItemStackRecipe itemRecipe = item.getRecipe();
        assertNotNull(itemRecipe);
        assertTrue(item.canOperate(itemRecipe));
        assertTrue(item.createNewCachedRecipe(itemRecipe, 0).isInputValid());

        TileEntityReplicatorGases gas = new TileEntityReplicatorGases();
        gas.inputTank.setGas(new GasStack(inputGas, 1));
        gas.uuTank.setGas(new GasStack(uuGas, 5));
        ReplicatorGasStackRecipe gasRecipe = gas.getRecipe();
        assertNotNull(gasRecipe);
        assertTrue(gas.canOperate(gasRecipe));
        assertTrue(gas.createNewCachedRecipe(gasRecipe, 0).isInputValid());

        TileEntityReplicatorFluidStack fluid = new TileEntityReplicatorFluidStack();
        fluid.inputTank.setFluid(new FluidStack(inputFluid, 1));
        fluid.uuTank.setGas(new GasStack(uuGas, 5));
        ReplicatorFluidStackRecipe fluidRecipe = fluid.getRecipe();
        assertNotNull(fluidRecipe);
        assertTrue(fluid.canOperate(fluidRecipe));
        assertTrue(fluid.createNewCachedRecipe(fluidRecipe, 0).isInputValid());
    }

    @Test
    void templateChangesRefreshConfigurationRevision() {
        TileEntityReplicatorItemStack item = configure(new TileEntityReplicatorItemStack());
        TileEntityReplicatorGases gas = configure(new TileEntityReplicatorGases());
        TileEntityReplicatorFluidStack fluid = configure(new TileEntityReplicatorFluidStack());
        int itemRevision = provider(item).getConfigurationRevision();
        int gasRevision = provider(gas).getConfigurationRevision();
        int fluidRevision = provider(fluid).getConfigurationRevision();

        item.getTemplateSlot().setStackUnchecked(new ItemStack(Items.IRON_INGOT, 2));
        gas.inputTank.setGas(new GasStack(inputGas, 11));
        fluid.inputTank.setFluid(new FluidStack(inputFluid, 101));

        assertNotEquals(itemRevision, provider(item).getConfigurationRevision());
        assertNotEquals(gasRevision, provider(gas).getConfigurationRevision());
        assertNotEquals(fluidRevision, provider(fluid).getConfigurationRevision());
    }

    @Test
    void rotaryModeSwitchChangesRoutesPortsAndRevision() {
        TileEntityTierRotaryCondensentrator tile = new TileEntityTierRotaryCondensentrator();
        MachineRecipeProviderRegistry.BoundProvider provider = provider(tile);
        int gasToFluidRevision = provider.getConfigurationRevision();
        List<MachineRecipeRoute> gasToFluid = provider.getRecipeRoutes();
        assertEquals(Arrays.asList("gas_input", "fluid_output"), portIds(provider));
        assertAllRoutePorts(gasToFluid, "gas_input", "fluid_output");

        tile.mode = false;
        int fluidToGasRevision = provider.getConfigurationRevision();
        List<MachineRecipeRoute> fluidToGas = provider.getRecipeRoutes();
        assertNotEquals(gasToFluidRevision, fluidToGasRevision);
        assertEquals(Arrays.asList("fluid_input", "gas_output"), portIds(provider));
        assertAllRoutePorts(fluidToGas, "fluid_input", "gas_output");
        assertTrue(provider.validateQIOConformance(QIOAutomationMode.SCHEDULED).isConformant());

        tile.mode = true;
        assertEquals(gasToFluidRevision, provider.getConfigurationRevision());
        assertEquals(gasToFluid.get(0).recipeKey(), provider.getRecipeRoutes().get(0).recipeKey());
    }

    @Test
    void tieredPresentationRestoresUltimateNameAndModelIdentityWithoutSplittingRecipeScope() {
        PresentationRotaryTile basic = new PresentationRotaryTile(MachineTier.BASIC);
        PresentationRotaryTile ultimate = new PresentationRotaryTile(MachineTier.ULTIMATE);
        MachineRecipeProviderRegistry.BoundProvider basicProvider = provider(basic);
        MachineRecipeProviderRegistry.BoundProvider ultimateProvider = provider(ultimate);

        MachinePresentationDescriptor basicPresentation = basicProvider.getPresentation();
        MachinePresentationDescriptor ultimatePresentation = ultimateProvider.getPresentation();
        NBTTagCompound presentationNbt = ultimatePresentation.getItemNbt();
        assertNotNull(presentationNbt);
        assertEquals(Collections.singleton("tier"), presentationNbt.getKeySet());
        assertEquals(MachineTier.ULTIMATE.ordinal(), presentationNbt.getInteger("tier"));

        ItemStack restored = new ItemStack(presentationItem, 1, ultimatePresentation.getItemMetadata());
        restored.setTagCompound(presentationNbt);
        assertEquals(BaseTier.ULTIMATE, ((ITierItem) restored.getItem()).getBaseTier(restored));
        assertNotEquals(basicPresentation.presentationKey(), ultimatePresentation.presentationKey());
        assertEquals("", basicProvider.getRecipeProfileScopeDiscriminator());
        assertEquals("", ultimateProvider.getRecipeProfileScopeDiscriminator());
    }

    @Test
    void qioUpgradesAreDynamicallySupportedAndPersistedByExistingUpgradeComponent() {
        QIOAutomationCapabilities.register();
        QIOAutomationUpgradeSupport.registerUpgradeSupport();
        for (TileEntity tile : processingTiles().values()) {
            IUpgradeTile processing = assertUpgradeHost(tile);
            assertTrue(processing.supportsUpgrade(QIOProcessingUpgrades.QIO_AUTO_CRAFTING));
            assertTrue(processing.supportsUpgrade(QIOProcessingUpgrades.QIO_AUTO_PROCESSING));
            assertTrue(processing.supportsUpgrade(QIOProcessingUpgrades.QIO_AUTO_OUTPUT));
        }
        for (TileEntity tile : outputTiles().values()) {
            IUpgradeTile output = assertUpgradeHost(tile);
            assertFalse(output.supportsUpgrade(QIOProcessingUpgrades.QIO_AUTO_CRAFTING));
            assertFalse(output.supportsUpgrade(QIOProcessingUpgrades.QIO_AUTO_PROCESSING));
            assertTrue(output.supportsUpgrade(QIOProcessingUpgrades.QIO_AUTO_OUTPUT));
        }

        assertUpgradeRoundTrip(TileEntityTierChemicalInfuser::new, QIOProcessingUpgrades.QIO_AUTO_CRAFTING);
        assertUpgradeRoundTrip(TileEntityTierChemicalInfuser::new, QIOProcessingUpgrades.QIO_AUTO_PROCESSING);
        assertUpgradeRoundTrip(TileEntityTierElectricPump::new, QIOProcessingUpgrades.QIO_AUTO_OUTPUT);
    }

    @Test
    void declaredMoreMachineProviderReceivesQIOCapability() {
        AttachCapabilitiesEvent<TileEntity> event = new AttachCapabilitiesEvent<>(TileEntity.class,
              new TileEntityTierChemicalInfuser());
        QIOAutomationEventHandler.INSTANCE.attachCapabilities(event);
        assertTrue(event.getCapabilities().containsKey(QIOAutomationCapabilities.NAME));
    }

    @Test
    void providerWithoutExplicitDeclarationDoesNotReceiveQIOCapability() {
        MachineRecipeProviderRegistry.unregister(UNREGISTERED_PROVIDER);
        try {
            MachineRecipeProviderRegistry.register(UNREGISTERED_PROVIDER, UnregisteredUpgradeTile.class,
                  new MachineRecipeProvider<UnregisteredUpgradeTile>() {
                      @Override
                      public List<MachineRecipeRoute> getRecipeRoutes(UnregisteredUpgradeTile tile) {
                          return Collections.singletonList(MachineRecipeRoute.builder("test:item_to_item")
                                .inputItem("input", new ItemStack(Items.IRON_INGOT))
                                .outputItem("output", new ItemStack(Items.GOLD_INGOT)).build());
                      }

                      @Override
                      public List<MachinePort> getPorts(UnregisteredUpgradeTile tile) {
                          return Arrays.asList(
                                MachinePort.item("input", MachinePort.Role.INPUT, tile.input),
                                MachinePort.item("output", MachinePort.Role.OUTPUT, tile.output));
                      }
                  });
            UnregisteredUpgradeTile tile = new UnregisteredUpgradeTile();
            assertFalse(provider(tile).getQIOConformance().isRegistered());
            AttachCapabilitiesEvent<TileEntity> event = new AttachCapabilitiesEvent<>(TileEntity.class, tile);
            QIOAutomationEventHandler.INSTANCE.attachCapabilities(event);
            assertTrue(event.getCapabilities().isEmpty());
        } finally {
            MachineRecipeProviderRegistry.unregister(UNREGISTERED_PROVIDER);
        }
    }

    @Test
    void voidMineralGeneratorExposesEveryOutputSlot() {
        MachineRecipeProviderRegistry.BoundProvider provider = provider(new TileEntityVoidMineralGenerator());
        assertEquals(81, provider.getPorts().size());
        assertEquals("item_output_0", provider.getPorts().get(0).portId());
        assertEquals("item_output_80", provider.getPorts().get(80).portId());
    }

    private static Map<String, TileEntity> processingTiles() {
        Map<String, TileEntity> tiles = new LinkedHashMap<>();
        tiles.put("tier_chemical_infuser", new TileEntityTierChemicalInfuser());
        tiles.put("tier_chemical_washer", new TileEntityTierChemicalWasher());
        tiles.put("tier_electrolytic_separator", new TileEntityTierElectrolyticSeparator());
        tiles.put("tier_isotopic_centrifuge", new TileEntityTierIsotopicCentrifuge());
        tiles.put("tier_rotary_condensentrator", new TileEntityTierRotaryCondensentrator());
        tiles.put("tier_solar_neutron_activator", new TileEntityTierSolarNeutronActivator());
        tiles.put("tier_chemical_crystallizer", new TileEntityTierChemicalCrystallizer(MachineTier.BASIC));
        tiles.put("tier_chemical_oxidizer", new TileEntityTierChemicalOxidizer(MachineTier.BASIC));
        tiles.put("tier_nutritional_liquifier", new TileEntityTierNutritionalLiquifier(MachineTier.BASIC));
        tiles.put("tier_chemical_dissolution_chamber", new TileEntityTierChemicalDissolutionChamber(MachineTier.BASIC));
        tiles.put("replicator_item", configure(new TileEntityReplicatorItemStack()));
        tiles.put("replicator_gas", configure(new TileEntityReplicatorGases()));
        tiles.put("replicator_fluid", configure(new TileEntityReplicatorFluidStack()));
        return tiles;
    }

    private static Map<String, TileEntity> outputTiles() {
        Map<String, TileEntity> tiles = new LinkedHashMap<>();
        tiles.put("tier_ambient_accumulator", new TileEntityTierAmbientAccumulator());
        tiles.put("tier_electric_pump", new TileEntityTierElectricPump());
        tiles.put("void_mineral_generator", new TileEntityVoidMineralGenerator());
        return tiles;
    }

    private static void addTestRecipes() {
        RecipeHandler.addChemicalInfuserRecipe(new GasStack(inputGas, 2), new GasStack(secondaryGas, 3),
              new GasStack(outputGas, 4));
        RecipeHandler.addChemicalWasherRecipe(new GasStack(inputGas, 2), new FluidStack(inputFluid, 5),
              new GasStack(outputGas, 4));
        RecipeHandler.addElectrolyticSeparatorRecipe(new FluidStack(inputFluid, 5), 10,
              new GasStack(inputGas, 2), new GasStack(outputGas, 3));
        RecipeHandler.addIsotopicRecipe(new GasStack(inputGas, 2), new GasStack(outputGas, 1));
        RecipeHandler.addRotaryRecipe(new FluidStack(inputFluid, 1), new GasStack(inputGas, 1),
              new GasStack(outputGas, 1), new FluidStack(inputFluid, 1));
        RecipeHandler.addSolarNeutronRecipe(new GasStack(inputGas, 2), new GasStack(outputGas, 1));
        RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(inputGas, 2), new ItemStack(Items.IRON_INGOT));
        RecipeHandler.addChemicalOxidizerRecipe(new ItemStack(Items.COAL), new GasStack(outputGas, 2));
        RecipeHandler.addNutritionalLiquifierRecipe(new ItemStack(Items.GOLD_INGOT), new GasStack(outputGas, 2));
        RecipeHandler.addChemicalDissolutionChamberRecipe(new ItemStack(Items.DIAMOND), new GasStack(outputGas, 2));
        RecipeHandler.addRecipe(RecipeHandler.Recipe.REPLICATOR_ITEMSTACK_RECIPE,
              new ReplicatorItemStackRecipe(new ItemStack(Items.IRON_INGOT), new GasStack(uuGas, 5),
                    new ItemStack(Items.IRON_INGOT), 0, 20));
        RecipeHandler.addRecipe(RecipeHandler.Recipe.REPLICATOR_GASES_RECIPE,
              new ReplicatorGasStackRecipe(new GasStack(inputGas, 10), new GasStack(uuGas, 5),
                    new GasStack(inputGas, 1), 0, 20));
        RecipeHandler.addRecipe(RecipeHandler.Recipe.REPLICATOR_FLUIDSTACK_RECIPE,
              new ReplicatorFluidStackRecipe(new FluidStack(inputFluid, 100), new GasStack(uuGas, 5),
                    new FluidStack(inputFluid, 1), 0, 20));
    }

    private static Gas gas(String name, int color) {
        Gas gas = GasRegistry.getGas(name);
        return gas == null ? GasRegistry.register(new Gas(name, color)) : gas;
    }

    private static Fluid fluid(String name) {
        Fluid fluid = FluidRegistry.getFluid(name);
        if (fluid == null) {
            fluid = new Fluid(name, new ResourceLocation("mekanism", "blocks/liquid/liquid"),
                  new ResourceLocation("mekanism", "blocks/liquid/liquid_flow"));
            assertTrue(FluidRegistry.registerFluid(fluid));
        }
        return fluid;
    }

    private static TileEntityReplicatorItemStack configure(TileEntityReplicatorItemStack tile) {
        tile.getTemplateSlot().setStack(new ItemStack(Items.IRON_INGOT));
        return tile;
    }

    private static TileEntityReplicatorGases configure(TileEntityReplicatorGases tile) {
        tile.inputTank.setGas(new GasStack(inputGas, 10));
        return tile;
    }

    private static TileEntityReplicatorFluidStack configure(TileEntityReplicatorFluidStack tile) {
        tile.inputTank.setFluid(new FluidStack(inputFluid, 100));
        return tile;
    }

    private static <T extends TileEntity> T configure(T tile) {
        return tile;
    }

    private static void assertTemplateProvider(MachineRecipeProviderRegistry.BoundProvider provider, String path,
          String outputPort, MachineResourceKind templateKind) {
        assertEquals(id(path), provider.id());
        assertEquals(Arrays.asList("template", "uu_input", outputPort), portIds(provider));
        MachinePort templatePort = provider.getPorts().get(0);
        assertEquals(MachinePort.Role.BOTH, templatePort.role());
        assertEquals(MachinePort.Purpose.CONFIGURATION, templatePort.purpose());
        assertEquals("template", templatePort.portGroupId());
        assertEquals(0, templatePort.laneId());
        assertEquals(templateKind, templatePort.kind());
        assertFalse(provider.getRecipeRoutes().isEmpty());
        for (MachineRecipeRoute route : provider.getRecipeRoutes()) {
            assertEquals(1, route.configurationInputs().size());
            assertEquals("template", route.configurationInputs().get(0).portId());
            assertEquals(templateKind, route.configurationInputs().get(0).kind());
            assertEquals(1, route.configurationInputs().get(0).amount());
            assertEquals(1, route.inputs().size());
            assertEquals("uu_input", route.inputs().get(0).portId());
            assertEquals(MachineResourceKind.GAS, route.inputs().get(0).kind());
            assertEquals(outputPort, route.guaranteedOutputs().get(0).portId());
        }
    }

    private static void assertLaneLayout(MachineRecipeProviderRegistry.BoundProvider provider, int lanes,
          boolean hasSharedInput) {
        assertTrue(provider.validateQIOConformance(QIOAutomationMode.SCHEDULED).isConformant(),
              provider.validateQIOConformance(QIOAutomationMode.SCHEDULED).errors().toString());
        Map<String, MachinePort> ports = new LinkedHashMap<>();
        for (MachinePort port : provider.getPorts()) {
            ports.put(port.portId(), port);
        }
        for (int lane = 0; lane < lanes; lane++) {
            List<MachineRecipeRoute> laneRoutes = new ArrayList<>();
            for (MachineRecipeRoute route : provider.getRecipeRoutes()) {
                if (route.recipeKey().endsWith(":lane_" + lane)) {
                    laneRoutes.add(route);
                }
            }
            assertFalse(laneRoutes.isEmpty(), provider.id() + " has no route for lane " + lane);
            for (MachineRecipeRoute route : laneRoutes) {
                List<MachineResourceStack> stacks = new ArrayList<>();
                stacks.addAll(route.inputs());
                stacks.addAll(route.guaranteedOutputs());
                stacks.addAll(route.optionalOutputs());
                for (MachineResourceStack stack : stacks) {
                    MachinePort port = ports.get(stack.portId());
                    assertNotNull(port, stack.portId());
                    if (hasSharedInput && "gas_input".equals(port.portId())) {
                        assertEquals(MachinePort.SHARED_LANE, port.laneId());
                    } else {
                        assertEquals(lane, port.laneId(), stack.portId());
                        assertTrue(port.portId().endsWith("_" + lane));
                    }
                }
            }
        }
    }

    private static void assertAllRoutePorts(List<MachineRecipeRoute> routes, String input, String output) {
        assertFalse(routes.isEmpty());
        for (MachineRecipeRoute route : routes) {
            assertEquals(input, route.inputs().get(0).portId());
            assertEquals(output, route.guaranteedOutputs().get(0).portId());
        }
    }

    private static IUpgradeTile assertUpgradeHost(TileEntity tile) {
        assertTrue(tile instanceof TileEntityContainerBlock);
        assertTrue(tile instanceof IUpgradeTile);
        return (IUpgradeTile) tile;
    }

    private static <T extends TileEntity & IUpgradeTile> void assertUpgradeRoundTrip(Supplier<T> factory,
          mekanism.common.Upgrade upgrade) {
        T source = factory.get();
        NBTTagCompound installed = new NBTTagCompound();
        Map<mekanism.common.Upgrade, Integer> upgrades = new LinkedHashMap<>();
        upgrades.put(upgrade, 1);
        mekanism.common.Upgrade.saveMap(upgrades, installed);
        NBTTagCompound component = new NBTTagCompound();
        component.setTag(mekanism.api.NBTConstants.COMPONENT_UPGRADE, installed);
        source.getComponent().read(component, false);
        assertTrue(source.isUpgradeInstalled(upgrade));
        NBTTagCompound nbt = new NBTTagCompound();
        source.getComponent().write(nbt);
        T restored = factory.get();
        restored.getComponent().read(nbt, false);
        assertTrue(restored.isUpgradeInstalled(upgrade));
    }

    private static List<String> portIds(MachineRecipeProviderRegistry.BoundProvider provider) {
        List<String> ids = new ArrayList<>();
        for (MachinePort port : provider.getPorts()) {
            ids.add(port.portId());
        }
        return ids;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("mekceumoremachine", path);
    }

    private static MachineRecipeProviderRegistry.BoundProvider provider(TileEntity tile) {
        MachineRecipeProviderRegistry.BoundProvider provider = MachineRecipeProviderRegistry.find(tile);
        assertNotNull(provider);
        return provider;
    }

    private static final class UnregisteredUpgradeTile extends TileEntityTierChemicalInfuser {

        private final mekanism.common.inventory.slot.BasicInventorySlot input =
              mekanism.common.inventory.slot.BasicInventorySlot.at(null, 0, 0);
        private final mekanism.common.inventory.slot.BasicInventorySlot output =
              mekanism.common.inventory.slot.BasicInventorySlot.at(null, 0, 0);

    }

    private static final class PresentationRotaryTile extends TileEntityTierRotaryCondensentrator {

        private PresentationRotaryTile(MachineTier tier) {
            this.tier = tier;
        }

        @Override
        public Block getBlockType() {
            return presentationBlock;
        }

        @Override
        public int getBlockMetadata() {
            return 0;
        }
    }
}
