package mekceumoremachine.common.processing;

import mekanism.api.processing.MachineRecipeProviderRegistry;
import mekceumoremachine.common.tile.machine.TileEntityVoidMineralGenerator;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorFluidStack;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorGases;
import mekceumoremachine.common.tile.machine.replicator.TileEntityReplicatorItemStack;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MoreMachineRecipeProvidersTest {

    @BeforeAll
    static void registerProviders() throws ReflectiveOperationException {
        Loader loader = Loader.instance();
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        if (namedMods.get(loader) == null) {
            namedMods.set(loader, Collections.emptyMap());
        }
        Bootstrap.register();
        MoreMachineRecipeProviders.register();
    }

    @Test
    void templateReplicatorsExposeOnlyConsumableInputAndOutputPorts() {
        assertProvider(new TileEntityReplicatorItemStack(), "replicator_item", 2, "uu_input", "item_output");
        assertProvider(new TileEntityReplicatorGases(), "replicator_gas", 2, "uu_input", "gas_output");
        assertProvider(new TileEntityReplicatorFluidStack(), "replicator_fluid", 2, "uu_input", "fluid_output");
    }

    @Test
    void voidMineralGeneratorExposesEveryOutputSlot() {
        MachineRecipeProviderRegistry.BoundProvider provider = provider(new TileEntityVoidMineralGenerator());

        assertEquals(new ResourceLocation("mekceumoremachine", "void_mineral_generator"), provider.id());
        assertEquals(81, provider.getPorts().size());
        assertEquals("item_output_0", provider.getPorts().get(0).portId());
        assertEquals("item_output_80", provider.getPorts().get(80).portId());
    }

    private static void assertProvider(TileEntity tile, String id, int portCount, String inputPort, String outputPort) {
        MachineRecipeProviderRegistry.BoundProvider provider = provider(tile);
        assertEquals(new ResourceLocation("mekceumoremachine", id), provider.id());
        assertEquals(portCount, provider.getPorts().size());
        assertEquals(inputPort, provider.getPorts().get(0).portId());
        assertEquals(outputPort, provider.getPorts().get(1).portId());
    }

    private static MachineRecipeProviderRegistry.BoundProvider provider(TileEntity tile) {
        MachineRecipeProviderRegistry.BoundProvider provider = MachineRecipeProviderRegistry.find(tile);
        assertNotNull(provider);
        return provider;
    }
}
