package mekceumoremachine.common.tile.machine;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IExtendedGasTank;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekceumoremachine.common.capability.ResizableGasTank;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierGasInputSorterConcurrencyTest {

    private static final Gas FIRST_GAS = new Gas("transaction_sort_test_first", 0x22AAFF);
    private static final Gas SECOND_GAS = new Gas("transaction_sort_test_second", 0xFFAA22);

    @Test
    void distributesSingleGasIntoEmptyTanksAndPreservesLockedAndFullTanks() {
        TestContext context = new TestContext(3, 10_000);
        context.firstMinimum = 4;
        TierGasInputSorter sorter = new TierGasInputSorter(context);
        context.tanks.get(0).setStackUnchecked(new GasStack(FIRST_GAS, 17));

        sorter.sort();
        assertEquals(17, context.stored(FIRST_GAS));
        assertTrue(context.nonEmptyTankCount() > 1, "valid empty process tanks should receive gas");

        context.locked[0] = true;
        int lockedAmount = context.tanks.get(0).getGasAmount();
        sorter.sort();
        assertEquals(lockedAmount, context.tanks.get(0).getGasAmount());

        context.locked[0] = false;
        context.runContainerTransaction(() -> {
            for (ResizableGasTank tank : context.tanks) {
                tank.setStackUnchecked(new GasStack(FIRST_GAS, tank.getCapacity()));
            }
        });
        GasStack input = new GasStack(FIRST_GAS, 1);
        GasStack remainder = context.callContainerTransaction(() ->
              context.tanks.get(0).insert(input, Action.EXECUTE, AutomationType.EXTERNAL));
        assertNotNull(remainder);
        assertEquals(1, remainder.amount);
        sorter.sort();
        assertEquals(30_000, context.stored(FIRST_GAS));
    }

    @Test
    void preservesMultipleGasesWhileSortingInsertingAndConsumingConcurrently() throws Exception {
        TestContext context = new TestContext(5, 10_000);
        context.firstMinimum = 4;
        context.secondMinimum = 7;
        TierGasInputSorter sorter = new TierGasInputSorter(context);
        context.tanks.get(0).setStackUnchecked(new GasStack(FIRST_GAS, 20));
        context.tanks.get(1).setStackUnchecked(new GasStack(FIRST_GAS, 12));
        context.tanks.get(2).setStackUnchecked(new GasStack(SECOND_GAS, 21));

        AtomicInteger acceptedFirst = new AtomicInteger(32);
        AtomicInteger acceptedSecond = new AtomicInteger(21);
        AtomicInteger consumedFirst = new AtomicInteger();
        AtomicInteger consumedSecond = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Future<?> sorting = executor.submit(() -> repeat(8_000, sorter::sort));
            Future<?> inserting = executor.submit(() -> {
                for (int i = 0; i < 8_000; i++) {
                    Gas gas = i % 3 == 0 ? SECOND_GAS : FIRST_GAS;
                    int tankIndex = i % context.tanks.size();
                    GasStack input = new GasStack(gas, i % 9 + 1);
                    GasStack remainder = context.callContainerTransaction(() ->
                          context.tanks.get(tankIndex).insert(input, Action.EXECUTE, AutomationType.EXTERNAL));
                    int inserted = input.amount - (remainder == null ? 0 : remainder.amount);
                    (gas == FIRST_GAS ? acceptedFirst : acceptedSecond).addAndGet(inserted);
                }
            });
            Future<?> consuming = executor.submit(() -> {
                for (int i = 0; i < 8_000; i++) {
                    int tankIndex = (i * 2) % context.tanks.size();
                    int amount = i % 5 + 1;
                    GasStack extracted = context.callContainerTransaction(() ->
                          context.tanks.get(tankIndex).extract(amount, Action.EXECUTE, AutomationType.INTERNAL));
                    if (extracted != null) {
                        (extracted.getGas() == FIRST_GAS ? consumedFirst : consumedSecond).addAndGet(extracted.amount);
                    }
                }
            });
            sorting.get(20, TimeUnit.SECONDS);
            inserting.get(20, TimeUnit.SECONDS);
            consuming.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(acceptedFirst.get(), context.stored(FIRST_GAS) + consumedFirst.get());
        assertEquals(acceptedSecond.get(), context.stored(SECOND_GAS) + consumedSecond.get());
    }

    private static void repeat(int count, Runnable action) {
        for (int i = 0; i < count; i++) {
            action.run();
        }
    }

    private static class TestContext extends TileEntityBasicBlock implements TierGasInputSorter.Context {

        private final List<ResizableGasTank> tanks = new ArrayList<>();
        private final boolean[] locked;
        private final boolean[] allowEmptyTarget;
        private int firstMinimum = 1;
        private int secondMinimum = 1;
        private final AtomicInteger sortChanges = new AtomicInteger();

        private TestContext(int processCount, int capacity) {
            locked = new boolean[processCount];
            allowEmptyTarget = new boolean[processCount];
            for (int process = 0; process < processCount; process++) {
                tanks.add(ResizableGasTank.create(capacity, gas -> true, null));
                allowEmptyTarget[process] = true;
            }
        }

        @Override
        public boolean isGasSorting() {
            return true;
        }

        @Override
        public int getGasSorterProcessCount() {
            return tanks.size();
        }

        @Override
        public IExtendedGasTank getGasSorterInputTank(int process) {
            return tanks.get(process);
        }

        @Override
        public boolean gasSorterInputProducesOutput(int process, Gas gas, boolean updateCache) {
            return allowEmptyTarget[process];
        }

        @Override
        public int getGasSorterNeededInput(Gas gas) {
            return gas == FIRST_GAS ? firstMinimum : secondMinimum;
        }

        @Override
        public void onGasSorterChanged() {
            sortChanges.incrementAndGet();
        }

        @Override
        public boolean isGasSorterProcessLocked(int process) {
            return locked[process];
        }

        private int stored(Gas gas) {
            return callContainerTransaction(() -> {
                int amount = 0;
                for (ResizableGasTank tank : tanks) {
                    GasStack stack = tank.getGas();
                    if (stack != null && stack.getGas() == gas) {
                        amount += stack.amount;
                    }
                }
                return amount;
            });
        }

        private int nonEmptyTankCount() {
            return callContainerTransaction(() -> {
                int count = 0;
                for (ResizableGasTank tank : tanks) {
                    if (!tank.isEmpty()) {
                        count++;
                    }
                }
                return count;
            });
        }
    }
}
