package mekceumoremachine.common.registries;


import mekanism.common.item.ItemMekanism;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.item.ItemCompositeTierInstaller;
import mekceumoremachine.common.item.ItemConnector;
import mekceumoremachine.common.item.ItemLargeMachineryUpgradeComponents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.registries.IForgeRegistry;

@ObjectHolder(MEKCeuMoreMachine.MODID)
public class MEKCeuMoreMachineItems {

    public static final Item CompositeTierInstaller = new ItemCompositeTierInstaller();
    public static final Item LargeMachineryUpgradeComponents = new ItemLargeMachineryUpgradeComponents();
    public static final Item UUMatter = new ItemMekanism().setRarity(EnumRarity.EPIC).setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
    public static final Item CONNECTOR = new ItemConnector();

    public static void registerItems(IForgeRegistry<Item> registry) {
        registry.register(init(CompositeTierInstaller, "CompositeTierInstaller"));
        if (Loader.isModLoaded("mekanismmultiblockmachine")) {
            registry.register(init(LargeMachineryUpgradeComponents, "LargeMachineryUpgradeComponents"));
        }
        registry.register(init(UUMatter,"UUMatter"));
        registry.register(init(CONNECTOR,"connector"));
    }

    public static Item init(Item item, String name) {
        return item.setTranslationKey(name).setRegistryName(new ResourceLocation(MEKCeuMoreMachine.MODID, name));
    }
}
