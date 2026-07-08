package mekceumoremachine.common.tile.machine;

import mekanism.api.Action;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class TierProcessInputSorter {

    private final Context context;
    private final Map<HashedItem, RecipeProcessInfo> processMap = new LinkedHashMap<>();
    private final List<RecipeProcessInfo> processes = new ArrayList<>();
    private final List<ProcessInfo> emptyProcesses = new ArrayList<>();

    public TierProcessInputSorter(Context context) {
        this.context = context;
    }

    public void sort() {
        if (!context.isSorting()) {
            return;
        }
        reset();
        collectProcesses();
        if (processes.isEmpty()) {
            return;
        }
        if (!emptyProcesses.isEmpty()) {
            addEmptySlotsAsTargets();
        }
        if (distributeItems()) {
            context.onSorterChanged();
        }
    }

    private void reset() {
        processes.clear();
        processMap.clear();
        emptyProcesses.clear();
    }

    private void collectProcesses() {
        for (int process = 0; process < context.getSorterProcessCount(); process++) {
            if (context.isSorterProcessLocked(process)) {
                continue;
            }
            IInventorySlot inputSlot = context.getSorterInputSlot(process);
            if (inputSlot == null) {
                continue;
            }
            ProcessInfo processInfo = new ProcessInfo(process, inputSlot);
            if (inputSlot.isEmpty()) {
                emptyProcesses.add(processInfo);
            } else {
                addInputProcess(processInfo, inputSlot.getStack());
            }
        }
    }

    private void addInputProcess(ProcessInfo processInfo, ItemStack inputStack) {
        RecipeProcessInfo recipeProcessInfo = getOrCreateProcessInfo(HashedItem.raw(inputStack));
        recipeProcessInfo.processes.add(processInfo);
        recipeProcessInfo.totalCount += inputStack.getCount();
        tryInitializeMinPerSlot(processInfo, recipeProcessInfo, inputStack);
    }

    private void tryInitializeMinPerSlot(ProcessInfo processInfo, RecipeProcessInfo recipeProcessInfo, ItemStack inputStack) {
        if (recipeProcessInfo.lazyMinPerSlot != null || context.areRecipeCachesInvalid()) {
            return;
        }
        ItemStack recipeInput = inputStack.copy();
        recipeProcessInfo.lazyMinPerSlot = () -> context.getSorterNeededInput(processInfo.process, recipeInput);
    }

    private RecipeProcessInfo getOrCreateProcessInfo(HashedItem item) {
        RecipeProcessInfo processInfo = processMap.get(item);
        if (processInfo == null) {
            processInfo = new RecipeProcessInfo(item);
            processMap.put(item, processInfo);
            processes.add(processInfo);
        }
        return processInfo;
    }

    private void addEmptySlotsAsTargets() {
        for (RecipeProcessInfo recipeProcessInfo : processes) {
            int emptyToAdd = getEmptyProcessTargetsToAdd(recipeProcessInfo);
            if (emptyToAdd <= 0) {
                continue;
            }
            int added = 0;
            List<ProcessInfo> toRemove = new ArrayList<>();
            for (ProcessInfo emptyProcess : emptyProcesses) {
                if (context.sorterInputProducesOutput(emptyProcess.process, recipeProcessInfo.item.getInternalStack(), true)) {
                    recipeProcessInfo.processes.add(emptyProcess);
                    toRemove.add(emptyProcess);
                    added++;
                    if (added >= emptyToAdd) {
                        break;
                    }
                }
            }
            emptyProcesses.removeAll(toRemove);
            if (emptyProcesses.isEmpty()) {
                break;
            }
        }
    }

    private int getEmptyProcessTargetsToAdd(RecipeProcessInfo recipeProcessInfo) {
        int minPerSlot = recipeProcessInfo.getMinPerSlot(context);
        int maxSlots = recipeProcessInfo.totalCount / minPerSlot;
        if (maxSlots <= 1) {
            return 0;
        }
        int processCount = recipeProcessInfo.processes.size();
        return maxSlots > processCount ? maxSlots - processCount : 0;
    }

    private boolean distributeItems() {
        boolean changed = false;
        for (RecipeProcessInfo recipeProcessInfo : processes) {
            int processCount = recipeProcessInfo.processes.size();
            if (processCount == 1) {
                continue;
            }
            DistributionState state = getInitialDistributionState(recipeProcessInfo, processCount);
            if (state.numberPerSlot == state.maxStackSize) {
                continue;
            }
            changed |= applyDistributionPlan(buildDistributionPlan(recipeProcessInfo, state, processCount));
        }
        return changed;
    }

    private List<DistributionPlan> buildDistributionPlan(RecipeProcessInfo recipeProcessInfo, DistributionState state, int processCount) {
        List<DistributionPlan> plan = new ArrayList<>(processCount);
        int remainder = state.remainder;
        for (int i = 0; i < processCount; i++) {
            ProcessInfo processInfo = recipeProcessInfo.processes.get(i);
            DistributionTarget target = getDistributionTarget(state.numberPerSlot, remainder, state.minPerSlot);
            plan.add(new DistributionPlan(processInfo.inputSlot, recipeProcessInfo.item, target.sizeForSlot));
            remainder = target.remainder;
        }
        return plan;
    }

    private boolean applyDistributionPlan(List<DistributionPlan> plan) {
        boolean changed = false;
        for (DistributionPlan target : plan) {
            changed |= applySlotDistribution(target.inputSlot, target.item, target.sizeForSlot);
        }
        return changed;
    }

    private boolean applySlotDistribution(IInventorySlot inputSlot, HashedItem item, int sizeForSlot) {
        if (inputSlot.isEmpty()) {
            if (sizeForSlot > 0) {
                inputSlot.setStack(item.createStack(sizeForSlot));
                return true;
            }
        } else if (sizeForSlot == 0) {
            inputSlot.setEmpty();
            return true;
        } else if (inputSlot.getCount() != sizeForSlot) {
            MekanismUtils.logMismatchedStackSize(sizeForSlot, inputSlot.setStackSize(sizeForSlot, Action.EXECUTE));
            return true;
        }
        return false;
    }

    private DistributionState getInitialDistributionState(RecipeProcessInfo recipeProcessInfo, int processCount) {
        HashedItem item = recipeProcessInfo.item;
        int maxStackSize = item.getInternalStack().getMaxStackSize();
        int numberPerSlot = recipeProcessInfo.totalCount / processCount;
        int remainder = recipeProcessInfo.totalCount % processCount;
        int minPerSlot = recipeProcessInfo.getMinPerSlot(context);
        if (minPerSlot > 1) {
            int perSlotRemainder = numberPerSlot % minPerSlot;
            if (perSlotRemainder > 0) {
                numberPerSlot -= perSlotRemainder;
                remainder += perSlotRemainder * processCount;
            }
            if (numberPerSlot + minPerSlot > maxStackSize) {
                minPerSlot = maxStackSize - numberPerSlot;
            }
        }
        return new DistributionState(maxStackSize, numberPerSlot, remainder, minPerSlot);
    }

    private DistributionTarget getDistributionTarget(int numberPerSlot, int remainder, int minPerSlot) {
        int sizeForSlot = numberPerSlot;
        if (remainder > 0) {
            if (remainder > minPerSlot) {
                sizeForSlot += minPerSlot;
                remainder -= minPerSlot;
            } else {
                sizeForSlot += remainder;
                remainder = 0;
            }
        }
        return new DistributionTarget(sizeForSlot, remainder);
    }

    public interface Context {

        boolean isSorting();

        int getSorterProcessCount();

        @Nullable
        IInventorySlot getSorterInputSlot(int process);

        boolean sorterInputProducesOutput(int process, ItemStack fallbackInput, boolean updateCache);

        int getSorterNeededInput(int process, ItemStack inputStack);

        boolean areRecipeCachesInvalid();

        void onSorterChanged();

        default boolean isSorterProcessLocked(int process) {
            return false;
        }
    }

    private static class ProcessInfo {

        private final int process;
        private final IInventorySlot inputSlot;

        private ProcessInfo(int process, IInventorySlot inputSlot) {
            this.process = process;
            this.inputSlot = inputSlot;
        }
    }

    private static class DistributionState {

        private final int maxStackSize;
        private final int numberPerSlot;
        private final int remainder;
        private final int minPerSlot;

        private DistributionState(int maxStackSize, int numberPerSlot, int remainder, int minPerSlot) {
            this.maxStackSize = maxStackSize;
            this.numberPerSlot = numberPerSlot;
            this.remainder = remainder;
            this.minPerSlot = minPerSlot;
        }
    }

    private static class DistributionTarget {

        private final int sizeForSlot;
        private final int remainder;

        private DistributionTarget(int sizeForSlot, int remainder) {
            this.sizeForSlot = sizeForSlot;
            this.remainder = remainder;
        }
    }

    private static class DistributionPlan {

        private final IInventorySlot inputSlot;
        private final HashedItem item;
        private final int sizeForSlot;

        private DistributionPlan(IInventorySlot inputSlot, HashedItem item, int sizeForSlot) {
            this.inputSlot = inputSlot;
            this.item = item;
            this.sizeForSlot = sizeForSlot;
        }
    }

    private static class RecipeProcessInfo {

        private final HashedItem item;
        private final List<ProcessInfo> processes = new ArrayList<>();
        @Nullable
        private IntSupplier lazyMinPerSlot;
        private int minPerSlot = 1;
        private int totalCount;

        private RecipeProcessInfo(HashedItem item) {
            this.item = item;
        }

        private int getMinPerSlot(Context context) {
            if (lazyMinPerSlot != null) {
                minPerSlot = Math.max(1, lazyMinPerSlot.getAsInt());
                lazyMinPerSlot = null;
            } else if (!processes.isEmpty()) {
                ItemStack largerInput = getLargerInputStack();
                minPerSlot = Math.max(1, context.getSorterNeededInput(processes.get(0).process, largerInput));
            }
            return minPerSlot;
        }

        private ItemStack getLargerInputStack() {
            return item.createStack(Math.min(item.getInternalStack().getMaxStackSize(), totalCount));
        }
    }
}
