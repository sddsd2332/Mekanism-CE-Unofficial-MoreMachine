package mekceumoremachine.client;

import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.ItemLayerWrapper;
import mekanism.client.render.tileentity.RenderConfigurableMachine;
import mekanism.common.base.ITierItem;
import mekceumoremachine.client.gui.*;
import mekceumoremachine.client.render.item.generator.RenderBigWindGeneratorItem;
import mekceumoremachine.client.render.item.generator.RenderTierWindGeneratorItem;
import mekceumoremachine.client.render.item.machine.RenderTierIsotopicCentrifugeItem;
import mekceumoremachine.client.render.item.machine.RenderTierSolarNeutronActivatorItem;
import mekceumoremachine.client.render.item.machine.RenderWirelessChargingStationItem;
import mekceumoremachine.client.render.tileentity.generator.RenderBigWindGenerator;
import mekceumoremachine.client.render.tileentity.generator.RenderTierWindGenerator;
import mekceumoremachine.client.render.tileentity.machine.RenderTierIsotopicCentrifuge;
import mekceumoremachine.client.render.tileentity.machine.RenderTierSolarNeutronActivator;
import mekceumoremachine.client.render.tileentity.machine.RenderWirelessChargingStation;
import mekceumoremachine.common.CommonProxy;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.registries.MEKCeuMoreMachineItems;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.generator.TileEntityBaseWindGenerator;
import mekceumoremachine.common.tile.generator.TileEntityBigWindGenerator;
import mekceumoremachine.common.tile.generator.TileEntityTierWindGenerator;
import mekceumoremachine.common.tile.machine.*;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
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

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {


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
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBigWindGenerator.class,new RenderBigWindGenerator());
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
        Item.getItemFromBlock(MEKCeuMoreMachineBlocks.BigWindGenerator).setTileEntityItemStackRenderer(new RenderBigWindGeneratorItem());
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
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MEKCeuMoreMachineBlocks.BigWindGenerator),0,getInventoryMRL("BigWindGenerator"));
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

        ModelResourceLocation BigWindGenerator = getInventoryMRL("BigWindGenerator");
        modelRegistry.putObject(BigWindGenerator,RenderBigWindGeneratorItem.model = new ItemLayerWrapper(modelRegistry.getObject(BigWindGenerator)));
    }

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public GuiScreen getClientGui(int ID, EntityPlayer player, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return switch (ID) {
            case 0 -> new GuiWirelessCharging(player.inventory, (TileEntityWirelessChargingStation) tileEntity);
            case 1 -> new GuiTierElectricPump(player.inventory, (TileEntityTierElectricPump) tileEntity);
            case 2 -> new GuiTierIsotopicCentrifuge(player.inventory, (TileEntityTierIsotopicCentrifuge) tileEntity);
            case 3 -> new GuiTierRotaryCondensentrator(player.inventory, (TileEntityTierRotaryCondensentrator) tileEntity);
            case 4 -> new GuiTierElectrolyticSeparator(player.inventory, (TileEntityTierElectrolyticSeparator) tileEntity);
            case 5 -> new GuiTierSolarNeutronActivator(player.inventory, (TileEntityTierSolarNeutronActivator) tileEntity);
            case 6 -> new GuiTierChemicalInfuser(player.inventory, (TileEntityTierChemicalInfuser) tileEntity);
            case 7 -> new GuiTierAmbientAccumulator(player.inventory, (TileEntityTierAmbientAccumulator) tileEntity);
            case 8 -> new GuiTierChemicalWasher(player.inventory, (TileEntityTierChemicalWasher) tileEntity);
            case 9 -> new GuiBaseWindGenerator(player.inventory,(TileEntityBaseWindGenerator) tileEntity);
            default -> null;
        };
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
    }
}
