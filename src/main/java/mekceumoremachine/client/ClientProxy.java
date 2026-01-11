package mekceumoremachine.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.tileentity.RenderConfigurableMachine;
import mekanism.common.base.ITierItem;
import mekceumoremachine.client.gui.*;
import mekceumoremachine.client.render.MEKCeuMoreMachineRenderer;
import mekceumoremachine.client.render.item.generator.RenderTierGasGeneratorItem;
import mekceumoremachine.client.render.item.generator.RenderTierWindGeneratorItem;
import mekceumoremachine.client.render.item.machine.*;
import mekceumoremachine.client.render.tileentity.generator.RenderTierGasGenerator;
import mekceumoremachine.client.render.tileentity.generator.RenderTierWindGenerator;
import mekceumoremachine.client.render.tileentity.machine.*;
import mekceumoremachine.common.CommonProxy;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.block.states.BlockStateTierChemicalOxidizer;
import mekceumoremachine.common.block.states.BlockStateTierChemicalOxidizer.tierChemicalOxidizerBlockStateMapper;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.registries.MEKCeuMoreMachineItems;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.generator.TileEntityBaseWindGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierGasGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;
import mekceumoremachine.common.tile.machine.TierDissolution.TileEntityTierChemicalDissolutionChamber;
import mekceumoremachine.common.tile.machine.TierNutritional.TileEntityTierNutritionalLiquifier;
import mekceumoremachine.common.tile.machine.TierOxidizer.TileEntityTierChemicalOxidizer;
import mekceumoremachine.common.tile.machine.*;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorFluidStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    private static final IStateMapper tierChemicalOxidizerMapper = new tierChemicalOxidizerBlockStateMapper();
    public static Map<String, ModelResourceLocation> tierChemicalOxidizerResources = new Object2ObjectOpenHashMap<>();

    @Override
    public void registerTESRs() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityWirelessChargingStation.class, new RenderWirelessChargingStation());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierIsotopicCentrifuge.class, RenderTierIsotopicCentrifuge.INSTANCE);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierRotaryCondensentrator.class, new RenderConfigurableMachine<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierElectrolyticSeparator.class, new RenderConfigurableMachine<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierSolarNeutronActivator.class, new RenderTierSolarNeutronActivator());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierChemicalInfuser.class, new RenderConfigurableMachine<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierAmbientAccumulator.class, new RenderConfigurableMachine<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierChemicalWasher.class, new RenderConfigurableMachine<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierWindGenerator.class, new RenderTierWindGenerator());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierChemicalDissolutionChamber.class, RenderTierChemicalDissolutionChamber.INSTANCE);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierNutritionalLiquifier.class, RenderTierNutritionalLiquifier.INSTANCE);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierChemicalOxidizer.class, new RenderConfigurableMachine<>());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTierGasGenerator.class, new RenderTierGasGenerator());

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityReplicatorItemStack.class, new RenderReplicatorItemStack());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityReplicatorGases.class, new RenderReplicatorGases());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityReplicatorFluidStack.class,new RenderReplicatorFluidStack());
    }

    @Override
    public void registerItemRenders() {
        registerItemRender(MEKCeuMoreMachineItems.CompositeTierInstaller);
        if (Loader.isModLoaded("mekanismmultiblockmachine")) {
            registerItemRender(MEKCeuMoreMachineItems.LargeMachineryUpgradeComponents);
        }
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.WirelessCharging).setTileEntityItemStackRenderer(new RenderWirelessChargingStationItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge).setTileEntityItemStackRenderer(new RenderTierIsotopicCentrifugeItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierSolarNeutronActivator).setTileEntityItemStackRenderer(new RenderTierSolarNeutronActivatorItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierWindGenerator).setTileEntityItemStackRenderer(new RenderTierWindGeneratorItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierChemicalDissolutionChamber).setTileEntityItemStackRenderer(new RenderTierChemicalDissolutionChamberItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierNutritionalLiquifier).setTileEntityItemStackRenderer(new RenderTierNutritionalLiquifierItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierGasGenerator).setTileEntityItemStackRenderer(new RenderTierGasGeneratorItem());

        registerItemRender(MEKCeuMoreMachineItems.UUMatter);
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.ReplicatorItemStack).setTileEntityItemStackRenderer(new RenderReplicatorItemStackItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.ReplicatorGases).setTileEntityItemStackRenderer(new RenderReplicatorGasesItem());
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.ReplicatorFluidStack).setTileEntityItemStackRenderer(new RenderReplicatorFluidStackItem());
    }

    @Override
    public void registerBlockRenders() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.WirelessCharging), 0, getInventoryMRL("WirelessCharging"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge), 0, getInventoryMRL("TierIsotopicCentrifuge"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierSolarNeutronActivator), 0, getInventoryMRL("TierSolarNeutronActivator"));
        addModel(MEKCeuMoreMachineBlocks.TierRotaryCondensentrator, "TierRotaryCondensentrator");
        addModel(MEKCeuMoreMachineBlocks.TierElectricPump, "TierElectricPump");
        addModel(MEKCeuMoreMachineBlocks.TierElectrolyticSeparator, "TierElectrolyticSeparator");
        addModel(MEKCeuMoreMachineBlocks.TierChemicalInfuser, "TierChemicalInfuser");
        addModel(MEKCeuMoreMachineBlocks.TierAmbientAccumulator, "TierAmbientAccumulator");
        addModel(MEKCeuMoreMachineBlocks.TierRadioactiveWasteBarrel, "TierRadioactiveWasteBarrel");
        addModel(MEKCeuMoreMachineBlocks.TierChemicalWasher, "TierChemicalWasher");
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierWindGenerator), 0, getInventoryMRL("TierWindGenerator"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierGasGenerator), 0, getInventoryMRL("TierGasGenerator"));

        for (int i = 0; i < 4; i++) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierChemicalDissolutionChamber), i, getInventoryMRL("TierChemicalDissolutionChamber"));
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierNutritionalLiquifier), i, getInventoryMRL("TierNutritionalLiquifier"));
        }
        registerTierChemicalOxidizerRenders();

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.ReplicatorItemStack), 0, getInventoryMRL("ReplicatorItemStack"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.ReplicatorGases), 0, getInventoryMRL("ReplicatorGases"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.ReplicatorFluidStack), 0, getInventoryMRL("ReplicatorFluidStack"));
    }


    private void registerTierChemicalOxidizerRenders() {
        ModelLoader.setCustomStateMapper(MEKCeuMoreMachineBlocks.TierChemicalOxidizer, tierChemicalOxidizerMapper);
        for (BlockStateTierChemicalOxidizer.MachineType type : BlockStateTierChemicalOxidizer.MachineType.values()) {
            if (!type.isValidMachine()) {
                continue;
            }
            List<ModelResourceLocation> modelsToAdd = new ArrayList<>();
            String resource = MEKCeuMoreMachine.MODID + ":" + type.getName();
            if (tierChemicalOxidizerResources.get(resource) == null) {
                List<String> entries = new ArrayList<>();
                if (type.hasActiveTexture()) {
                    entries.add("active=false");
                }
                if (type.hasRotations()) {
                    entries.add("facing=north");
                }

                String properties = getProperties(entries);
                ModelResourceLocation model = new ModelResourceLocation(resource, properties);
                tierChemicalOxidizerResources.put(resource, model);
                modelsToAdd.add(model);
            }
            ModelLoader.registerItemVariants(Item.getItemFromBlock(type.typeBlock.getBlock()), modelsToAdd.toArray(new ModelResourceLocation[]{}));
        }

        ItemMeshDefinition machineMesher = stack -> {
            BlockStateTierChemicalOxidizer.MachineType type = BlockStateTierChemicalOxidizer.MachineType.get(stack);
            if (type != null) {
                String resource = MEKCeuMoreMachine.MODID + ":" + type.getName();
                return tierChemicalOxidizerResources.get(resource);
            }
            return null;
        };
        ModelLoader.setCustomMeshDefinition(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.TierChemicalOxidizer), machineMesher);
    }


    private String getProperties(List<String> entries) {
        StringBuilder properties = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            properties.append(entries.get(i));
            if (i < entries.size() - 1) {
                properties.append(",");
            }
        }
        return properties.toString();
    }


    public void addModel(Block block, String type) {
        ModelLoader.setCustomMeshDefinition(Item.getItemFromBlock(block), stack -> {
            MachineTier tier = MachineTier.values()[((ITierItem) stack.getItem()).getBaseTier(stack).ordinal()];
            ResourceLocation baseLocation = new ResourceLocation(MEKCeuMoreMachine.MODID, type);
            return new ModelResourceLocation(baseLocation, "facing=north,tier=" + tier);
        });
    }


    private ModelResourceLocation getInventoryMRL(String type) {
        return new ModelResourceLocation(new ResourceLocation(MEKCeuMoreMachine.MODID, type), "inventory");
    }

    public void registerItemRender(Item item) {
        MekanismRenderer.registerItemRender(MEKCeuMoreMachine.MODID, item);
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        IRegistry<ModelResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();
        ModelResourceLocation WirelessCharging = getInventoryMRL("WirelessCharging");
        modelRegistry.putObject(WirelessCharging, RenderWirelessChargingStationItem.model = new ItemLayerWrapper(modelRegistry.getObject(WirelessCharging)));

        ModelResourceLocation TierIsotopicCentrifuge = getInventoryMRL("TierIsotopicCentrifuge");
        modelRegistry.putObject(TierIsotopicCentrifuge, RenderTierIsotopicCentrifugeItem.model = new ItemLayerWrapper(modelRegistry.getObject(TierIsotopicCentrifuge)));

        ModelResourceLocation TierSolarNeutronActivator = getInventoryMRL("TierSolarNeutronActivator");
        modelRegistry.putObject(TierSolarNeutronActivator, RenderTierSolarNeutronActivatorItem.model = new ItemLayerWrapper(modelRegistry.getObject(TierSolarNeutronActivator)));

        ModelResourceLocation TierWindGenerator = getInventoryMRL("TierWindGenerator");
        modelRegistry.putObject(TierWindGenerator, RenderTierWindGeneratorItem.model = new ItemLayerWrapper(modelRegistry.getObject(TierWindGenerator)));

        ModelResourceLocation TierChemicalDissolutionChamber = getInventoryMRL("TierChemicalDissolutionChamber");
        modelRegistry.putObject(TierChemicalDissolutionChamber, RenderTierChemicalDissolutionChamberItem.model = new ItemLayerWrapper(modelRegistry.getObject(TierChemicalDissolutionChamber)));

        ModelResourceLocation TierNutritionalLiquifier = getInventoryMRL("TierNutritionalLiquifier");
        modelRegistry.putObject(TierNutritionalLiquifier, RenderTierNutritionalLiquifierItem.model = new ItemLayerWrapper(modelRegistry.getObject(TierNutritionalLiquifier)));

        ModelResourceLocation TierGasGenerator = getInventoryMRL("TierGasGenerator");
        modelRegistry.putObject(TierGasGenerator, RenderTierGasGeneratorItem.model = new ItemLayerWrapper(modelRegistry.getObject(TierGasGenerator)));

        ModelResourceLocation ReplicatorItemStack = getInventoryMRL("ReplicatorItemStack");
        modelRegistry.putObject(ReplicatorItemStack, RenderReplicatorItemStackItem.model = new ItemLayerWrapper(modelRegistry.getObject(ReplicatorItemStack)));

        ModelResourceLocation ReplicatorGases = getInventoryMRL("ReplicatorGases");
        modelRegistry.putObject(ReplicatorGases, RenderReplicatorGasesItem.model = new ItemLayerWrapper(modelRegistry.getObject(ReplicatorGases)));

        ModelResourceLocation ReplicatorFluidStack = getInventoryMRL("ReplicatorFluidStack");
        modelRegistry.putObject(ReplicatorFluidStack, RenderReplicatorFluidStackItem.model = new ItemLayerWrapper(modelRegistry.getObject(ReplicatorFluidStack)));
    }

    @Override
    public void preInit() {
        MEKCeuMoreMachineRenderer.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public GuiScreen getClientGui(int ID, EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return switch (ID) {
            case 0 -> new GuiWirelessCharging(player.inventory, (TileEntityWirelessChargingStation) tileEntity);
            case 1 -> new GuiTierElectricPump(player.inventory, (TileEntityTierElectricPump) tileEntity);
            case 2 -> new GuiTierIsotopicCentrifuge(player.inventory, (TileEntityTierIsotopicCentrifuge) tileEntity);
            case 3 ->
                    new GuiTierRotaryCondensentrator(player.inventory, (TileEntityTierRotaryCondensentrator) tileEntity);
            case 4 ->
                    new GuiTierElectrolyticSeparator(player.inventory, (TileEntityTierElectrolyticSeparator) tileEntity);
            case 5 ->
                    new GuiTierSolarNeutronActivator(player.inventory, (TileEntityTierSolarNeutronActivator) tileEntity);
            case 6 -> new GuiTierChemicalInfuser(player.inventory, (TileEntityTierChemicalInfuser) tileEntity);
            case 7 -> new GuiTierAmbientAccumulator(player.inventory, (TileEntityTierAmbientAccumulator) tileEntity);
            case 8 -> new GuiTierChemicalWasher(player.inventory, (TileEntityTierChemicalWasher) tileEntity);
            case 9 -> new GuiBaseWindGenerator(player.inventory, (TileEntityBaseWindGenerator) tileEntity);
            case 10 ->
                    new GuiTierChemicalDissolutionChamber(player.inventory, (TileEntityTierChemicalDissolutionChamber) tileEntity);
            case 11 ->
                    new GuiTierNutritionalLiquifier(player.inventory, (TileEntityTierNutritionalLiquifier) tileEntity);
            case 12 -> new GuiTierChemicalOxidizer(player.inventory, (TileEntityTierChemicalOxidizer) tileEntity);
            case 13 -> new GuiTierGasGenerator(player.inventory, (TileEntityTierGasGenerator) tileEntity);
            case 14 -> new GuiReplicatorItemStack(player.inventory, (TileEntityReplicatorItemStack) tileEntity);
            case 15 -> new GuiReplicatorGases(player.inventory,(TileEntityReplicatorGases) tileEntity);
            case 16 -> new GuiReplicatorFluidStack(player.inventory,(TileEntityReplicatorFluidStack) tileEntity);
            default -> null;
        };
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
    }
}
