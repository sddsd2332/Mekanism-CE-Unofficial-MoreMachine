package mekceumoremachine.common.tile.machine;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierProcessInputSorterConcurrencyTest {

    private static final Item TEST_ITEM = new Item();

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void preservesNbtItemsWhileSortingInsertingAndConsumingConcurrently() throws Exception {
        TestContext context = new TestContext(7, 4);
        TierProcessInputSorter sorter = new TierProcessInputSorter(context);
        ItemStack first = stack(1, 12);
        ItemStack second = stack(2, 9);
        context.slots.get(0).setStack(first);
        context.slots.get(1).setStack(stack(1, 4));
        context.slots.get(2).setStack(second);

        AtomicInteger acceptedFirst = new AtomicInteger(16);
        AtomicInteger acceptedSecond = new AtomicInteger(9);
        AtomicInteger consumedFirst = new AtomicInteger();
        AtomicInteger consumedSecond = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Future<?> sorting = executor.submit(() -> repeat(8_000, sorter::sort));
            Future<?> inserting = executor.submit(() -> {
                for (int i = 0; i < 8_000; i++) {
                    int variant = i % 2 + 1;
                    ItemStack input = stack(variant, i % 5 + 1);
                    int slot = (i * 3) % context.slots.size();
                    ItemStack remainder = context.callContainerTransaction(() ->
                          context.slots.get(slot).insertItem(input, Action.EXECUTE, AutomationType.EXTERNAL));
                    int inserted = input.getCount() - remainder.getCount();
                    (variant == 1 ? acceptedFirst : acceptedSecond).addAndGet(inserted);
                }
            });
            Future<?> consuming = executor.submit(() -> {
                for (int i = 0; i < 8_000; i++) {
                    int slot = (i * 5) % context.slots.size();
                    int amount = i % 3 + 1;
                    ItemStack extracted = context.callContainerTransaction(() ->
                          context.slots.get(slot).extractItem(amount, Action.EXECUTE, AutomationType.INTERNAL));
                    if (!extracted.isEmpty()) {
                        (variant(extracted) == 1 ? consumedFirst : consumedSecond).addAndGet(extracted.getCount());
                    }
                }
            });
            sorting.get(20, TimeUnit.SECONDS);
            inserting.get(20, TimeUnit.SECONDS);
            consuming.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        int[] stored = context.callContainerTransaction(context::storedByVariant);
        assertEquals(acceptedFirst.get(), stored[0] + consumedFirst.get());
        assertEquals(acceptedSecond.get(), stored[1] + consumedSecond.get());
        assertTrue(context.sortChanges.get() > 0);
    }

    @Test
    void respectsCapacityEmptyTargetsMinimumInputAndLockedProcesses() {
        TestContext context = new TestContext(5, 4);
        TierProcessInputSorter sorter = new TierProcessInputSorter(context);
        context.slots.get(0).setStack(stack(1, 10));
        context.locked[0] = true;
        context.slots.get(1).setStack(stack(2, 64));
        context.allowEmptyTarget[4] = false;

        sorter.sort();

        assertEquals(10, context.slots.get(0).getCount(), "an in-progress process must not be redistributed");
        assertTrue(context.slots.get(4).isEmpty(), "an invalid recipe target must stay empty");
        int secondVariantTotal = 0;
        for (BasicInventorySlot slot : context.slots) {
            assertTrue(slot.getCount() <= 64, "sorting must not exceed slot capacity");
            if (!slot.isEmpty() && variant(slot.getStack()) == 2) {
                secondVariantTotal += slot.getCount();
            }
        }
        assertEquals(64, secondVariantTotal, "redistributing a full slot must preserve its total");

        context.locked[0] = false;
        context.slots.get(1).setEmpty();
        sorter.sort();
        int total = 0;
        for (BasicInventorySlot slot : context.slots) {
            if (!slot.isEmpty() && variant(slot.getStack()) == 1) {
                total += slot.getCount();
            }
        }
        assertEquals(10, total);
    }

    private static ItemStack stack(int variant, int amount) {
        ItemStack stack = new ItemStack(TEST_ITEM, amount);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("variant", variant);
        stack.setTagCompound(tag);
        return stack;
    }

    private static int variant(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getInteger("variant") : 0;
    }

    private static void repeat(int count, Runnable action) {
        for (int i = 0; i < count; i++) {
            action.run();
        }
    }

    private static class TestContext extends TileEntityBasicBlock implements TierProcessInputSorter.Context {

        private final List<BasicInventorySlot> slots = new ArrayList<>();
        private final boolean[] locked;
        private final boolean[] allowEmptyTarget;
        private final int minimumInput;
        private final AtomicInteger sortChanges = new AtomicInteger();

        private TestContext(int processCount, int minimumInput) {
            this.minimumInput = minimumInput;
            locked = new boolean[processCount];
            allowEmptyTarget = new boolean[processCount];
            for (int process = 0; process < processCount; process++) {
                slots.add(BasicInventorySlot.at(null, 0, 0));
                allowEmptyTarget[process] = true;
            }
        }

        @Override
        public boolean isSorting() {
            return true;
        }

        @Override
        public int getSorterProcessCount() {
            return slots.size();
        }

        @Override
        public IInventorySlot getSorterInputSlot(int process) {
            return slots.get(process);
        }

        @Override
        public boolean sorterInputProducesOutput(int process, ItemStack fallbackInput, boolean updateCache) {
            return allowEmptyTarget[process];
        }

        @Override
        public int getSorterNeededInput(int process, ItemStack inputStack) {
            return minimumInput;
        }

        @Override
        public boolean areRecipeCachesInvalid() {
            return false;
        }

        @Override
        public void onSorterChanged() {
            sortChanges.incrementAndGet();
        }

        @Override
        public boolean isSorterProcessLocked(int process) {
            return locked[process];
        }

        private int[] storedByVariant() {
            int[] stored = new int[2];
            for (BasicInventorySlot slot : slots) {
                if (!slot.isEmpty()) {
                    int variant = variant(slot.getStack());
                    if (variant == 1 || variant == 2) {
                        stored[variant - 1] += slot.getCount();
                    }
                }
            }
            return stored;
        }
    }
}
