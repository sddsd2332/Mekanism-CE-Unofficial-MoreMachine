package mekceumoremachine.common.capability;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.minecraft.init.Bootstrap;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResizableTankSnapshotTest {

    private static Gas testGas;

    @BeforeAll
    static void registerTestGas() {
        Bootstrap.register();
        testGas = GasRegistry.getGas("more_machine_snapshot_test");
        if (testGas == null) {
            testGas = GasRegistry.register(new Gas("more_machine_snapshot_test", 0x4070A0));
        }
    }

    @Test
    void gasSnapshotRestoreDoesNotNotifyListeners() {
        AtomicInteger changes = new AtomicInteger();
        ResizableGasTank tank = ResizableGasTank.create(1_000, gas -> true, changes::incrementAndGet);
        tank.insert(new GasStack(testGas, 100), Action.EXECUTE, AutomationType.INTERNAL);
        int afterInitialInsert = changes.get();
        net.minecraft.nbt.NBTTagCompound snapshot = tank.createContentsSnapshot();
        tank.insert(new GasStack(testGas, 50), Action.EXECUTE, AutomationType.INTERNAL);
        int beforeRestore = changes.get();

        tank.restoreContentsSnapshot(snapshot);

        assertEquals(afterInitialInsert + 1, beforeRestore);
        assertEquals(beforeRestore, changes.get());
        assertNotNull(tank.getGas());
        assertEquals(100, tank.getStored());
    }

    @Test
    void fluidSnapshotRestoreDoesNotNotifyListeners() {
        AtomicInteger changes = new AtomicInteger();
        ResizableFluidTank tank = ResizableFluidTank.create(1_000, fluid -> true, changes::incrementAndGet);
        tank.insert(new FluidStack(FluidRegistry.WATER, 100), Action.EXECUTE, AutomationType.INTERNAL);
        int afterInitialInsert = changes.get();
        net.minecraft.nbt.NBTTagCompound snapshot = tank.createContentsSnapshot();
        tank.insert(new FluidStack(FluidRegistry.WATER, 50), Action.EXECUTE, AutomationType.INTERNAL);
        int beforeRestore = changes.get();

        tank.restoreContentsSnapshot(snapshot);

        assertEquals(afterInitialInsert + 1, beforeRestore);
        assertEquals(beforeRestore, changes.get());
        assertNotNull(tank.getFluid());
        assertEquals(100, tank.getFluidAmount());
    }

    @Test
    void legacyGasTransfersUseExtendedTankRulesAndNotifyListeners() {
        AtomicInteger changes = new AtomicInteger();
        ResizableGasTank tank = ResizableGasTank.create(1_000, gas -> true, changes::incrementAndGet);
        GasStack oversized = new GasStack(testGas, 1_200);

        assertEquals(1_000, tank.receive(oversized, false));
        assertEquals(0, tank.getStored());
        assertEquals(0, changes.get());

        assertEquals(1_000, tank.receive(oversized, true));
        assertEquals(1_000, tank.getStored());
        assertEquals(1, changes.get());

        assertEquals(250, tank.draw(250, false).amount);
        assertEquals(1_000, tank.getStored());
        assertEquals(1, changes.get());

        assertEquals(250, tank.draw(250, true).amount);
        assertEquals(750, tank.getStored());
        assertEquals(2, changes.get());
    }

    @Test
    void legacyFluidTransfersUseExtendedTankRulesAndNotifyListeners() {
        AtomicInteger changes = new AtomicInteger();
        ResizableFluidTank tank = ResizableFluidTank.create(1_000, fluid -> true, changes::incrementAndGet);
        FluidStack oversized = new FluidStack(FluidRegistry.WATER, 1_200);

        assertEquals(1_000, tank.fill(oversized, false));
        assertEquals(0, tank.getFluidAmount());
        assertEquals(0, changes.get());

        assertEquals(1_000, tank.fill(oversized, true));
        assertEquals(1_000, tank.getFluidAmount());
        assertEquals(1, changes.get());

        assertEquals(250, tank.drain(new FluidStack(FluidRegistry.WATER, 250), false).amount);
        assertEquals(1_000, tank.getFluidAmount());
        assertEquals(1, changes.get());

        assertEquals(250, tank.drain(250, true).amount);
        assertEquals(750, tank.getFluidAmount());
        assertEquals(2, changes.get());
    }
}
