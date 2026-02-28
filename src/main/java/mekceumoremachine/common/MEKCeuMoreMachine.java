package mekceumoremachine.common;

import ic2.core.ref.FluidName;
import io.netty.buffer.ByteBuf;
import mekanism.api.MekanismAPI;
import mekanism.common.Mekanism;
import mekanism.common.Version;
import mekanism.common.base.IModule;
import mekanism.common.config.MekanismConfig;
import mekanism.common.network.PacketSimpleGui;
import mekceumoremachine.common.capability.DefaultLinkCapability;
import mekceumoremachine.common.capability.LinkTileEntity;
import mekceumoremachine.common.config.MoreMachineConfig;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.registries.MEKCeuMoreMachineFluids;
import mekceumoremachine.common.registries.MEKCeuMoreMachineItems;
import mekceumoremachine.mekceumoremachine.Tags;
import mekceumoremachine.common.util.VoidMineralGeneratorUitls;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.io.File;
import java.util.Optional;

@Mod(modid = MEKCeuMoreMachine.MODID, useMetadata = true,customProperties = {
        @Mod.CustomProperty(k = "license", v = "MIT"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/sddsd2332/Mekanism-CE-Unofficial-MoreMachine/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/mekceumoremachine/icon.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/mekceumoremachine/background.png")
})
@Mod.EventBusSubscriber()
public class MEKCeuMoreMachine implements IModule {

    public static final String MODID = Tags.MOD_ID;

    @SidedProxy(clientSide = "mekceumoremachine.client.ClientProxy", serverSide = "mekceumoremachine.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance(MEKCeuMoreMachine.MODID)
    public static MEKCeuMoreMachine instance;

    public static Version versionNumber = new Version(999, 999, 999);

    public static CreativeTabMEKCeuMoreMachine tabMEKCeuMoreMachine = new CreativeTabMEKCeuMoreMachine();

    public static Configuration config;

    static {
        MEKCeuMoreMachineFluids.register();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        MEKCeuMoreMachineBlocks.registerBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        MEKCeuMoreMachineItems.registerItems(event.getRegistry());
        MEKCeuMoreMachineBlocks.registerItemBlocks(event.getRegistry());
    }


    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // Register models
        proxy.registerBlockRenders();
        proxy.registerItemRenders();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
        config = new Configuration(new File("config/mekanism/MoreMekMachine.cfg"));
        loadConfiguration();
        DefaultLinkCapability.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        //Add this module to the core list
        Mekanism.modulesLoaded.add(this);
        //Register this module's GUI handler in the simple packet protocol
        PacketSimpleGui.handlers.add(proxy);
        //Set up the GUI handler
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new MEKCeuMoreMachineGuiHandler());
        MinecraftForge.EVENT_BUS.register(this);

        //Load the proxy
        proxy.registerTileEntities();
        proxy.registerTESRs();
        proxy.init();

        //和IC2兼容
        if (Mekanism.hooks.IC2Loaded) {
            MEKCeuMoreMachineFluids.UU_MATTER.setFluid(FluidName.uu_matter.getInstance());
        }

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Populate the ore list after all mods have had a chance to register to the OreDictionary
        VoidMineralGeneratorUitls.populateCanOre();
    }

    @Override
    public Version getVersion() {
        return versionNumber;
    }

    @Override
    public String getName() {
        return "MoreMachine";
    }

    @Override
    public void writeConfig(ByteBuf byteBuf, MekanismConfig mekanismConfig) {

    }

    @Override
    public void readConfig(ByteBuf byteBuf, MekanismConfig mekanismConfig) {

    }

    @Override
    public void resetClient() {

    }


    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MEKCeuMoreMachine.MODID) || event.getModID().equals(Mekanism.MODID)) {
            loadConfiguration();
        }
    }

    @SubscribeEvent
    public void onBlacklistUpdate(MekanismAPI.BoxBlacklistEvent event) {
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.WirelessCharging, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.TierSolarNeutronActivator, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.TierWindGenerator, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.WirelessEnergy, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.TierAdvancedSolarGenerator,0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.VoidMineralGenerator,0);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        MEKCeuMoreMachineRecipes.addRecipes();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void removeRecipes(RegistryEvent.Register<IRecipe> event) {
        MEKCeuMoreMachineRecipes.removeRecipes();
    }

    public void loadConfiguration() {
        MoreMachineConfig.local().config.load(config);
        if (config.hasChanged()) {
            config.save();
        }
    }


    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<TileEntity> event) {
        if (event.getObject() instanceof TileEntity) {
            DefaultLinkCapability.Provider radiationProvider = new DefaultLinkCapability.Provider();
            event.addCapability(DefaultLinkCapability.Provider.NAME, radiationProvider);
        }
    }

    public static Optional<LinkTileEntity> getLinkInfoCap(TileEntity entity) {
        if (entity.hasCapability(DefaultLinkCapability.Provider.LINK_CAPABILITY, null)) {
            return Optional.ofNullable(entity.getCapability(DefaultLinkCapability.Provider.LINK_CAPABILITY, null));
        } else {
            return Optional.empty();
        }

    }


}
