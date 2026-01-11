package mekceumoremachine.common;

import ic2.core.ref.FluidName;
import io.netty.buffer.ByteBuf;
import mekanism.api.MekanismAPI;
import mekanism.common.Mekanism;
import mekanism.common.Version;
import mekanism.common.base.IModule;
import mekanism.common.config.MekanismConfig;
import mekanism.common.network.PacketSimpleGui;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.registries.MEKCeuMoreMachineFluids;
import mekceumoremachine.common.registries.MEKCeuMoreMachineItems;
import mekceumoremachine.mekceumoremachine.Tags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = MEKCeuMoreMachine.MODID, useMetadata = true)
@Mod.EventBusSubscriber()
public class MEKCeuMoreMachine implements IModule {

    public static final String MODID = Tags.MOD_ID;

    @SidedProxy(clientSide = "mekceumoremachine.client.ClientProxy", serverSide = "mekceumoremachine.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance(MEKCeuMoreMachine.MODID)
    public static MEKCeuMoreMachine instance;

    public static Version versionNumber = new Version(999, 999, 999);

    public static CreativeTabMEKCeuMoreMachine tabMEKCeuMoreMachine = new CreativeTabMEKCeuMoreMachine();


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
        proxy.loadConfiguration();
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
            proxy.loadConfiguration();
        }
    }

    @SubscribeEvent
    public void onBlacklistUpdate(MekanismAPI.BoxBlacklistEvent event) {
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.WirelessCharging, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.TierIsotopicCentrifuge, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.TierSolarNeutronActivator, 0);
        MekanismAPI.addBoxBlacklist(MEKCeuMoreMachineBlocks.TierWindGenerator, 0);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        MEKCeuMoreMachineRecipes.addRecipes();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void removeRecipes(RegistryEvent.Register<IRecipe> event) {
        MEKCeuMoreMachineRecipes.removeRecipes();
    }
}
