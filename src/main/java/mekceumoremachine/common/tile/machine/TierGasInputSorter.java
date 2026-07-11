package mekceumoremachine.common.tile.machine;

import mekanism.api.IContainerTransaction;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IExtendedGasTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Distributes gas inputs between equivalent machine processes.
 */
public class TierGasInputSorter {

    private final Context context;

    public TierGasInputSorter(Context context) {
        this.context = context;
    }

    public void sort() {
        context.runContainerTransaction(this::sortInTransaction);
    }

    private void sortInTransaction() {
        if (!context.isGasSorting()) {
            return;
        }
        Map<Gas, GasDistributionGroup> groups = new LinkedHashMap<>();
        List<GasProcessTarget> emptyTargets = new ArrayList<>();
        for (int process = 0; process < context.getGasSorterProcessCount(); process++) {
            if (context.isGasSorterProcessLocked(process)) {
                continue;
            }
            IExtendedGasTank tank = context.getGasSorterInputTank(process);
            if (tank == null) {
                continue;
            }
            GasProcessTarget target = new GasProcessTarget(process, tank);
            GasStack stack = tank.getGas();
            if (stack == null || stack.amount <= 0) {
                emptyTargets.add(target);
            } else {
                groups.computeIfAbsent(stack.getGas(), GasDistributionGroup::new).add(target, stack.amount);
            }
        }
        if (groups.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (GasDistributionGroup group : groups.values()) {
            changed |= distributeGasGroup(group, emptyTargets);
        }
        if (changed) {
            context.onGasSorterChanged();
        }
    }

    private boolean distributeGasGroup(GasDistributionGroup group, List<GasProcessTarget> emptyTargets) {
        int targetCount = getGasDistributionTargetCount(group, emptyTargets.size());
        if (targetCount <= 0) {
            return false;
        }
        List<GasProcessTarget> targets = new ArrayList<>(targetCount);
        for (int i = 0; i < Math.min(targetCount, group.targets.size()); i++) {
            targets.add(group.targets.get(i));
        }
        for (int i = 0; i < emptyTargets.size() && targets.size() < targetCount; ) {
            GasProcessTarget emptyTarget = emptyTargets.get(i);
            if (context.gasSorterInputProducesOutput(emptyTarget.process, group.gas, true)) {
                targets.add(emptyTarget);
                emptyTargets.remove(i);
            } else {
                i++;
            }
        }
        if (targets.isEmpty()) {
            return false;
        }
        List<GasDistributionPlan> plan = buildGasDistributionPlan(group, targets);
        return isGasDistributionPlanValid(group, plan) && applyGasDistributionPlan(group, plan, emptyTargets);
    }

    private int getGasDistributionTargetCount(GasDistributionGroup group, int emptyTankCount) {
        int minPerTank = getMinimumInput(group.gas);
        int maxTargetsByAmount = Math.max(1, group.totalAmount / minPerTank);
        int maxTargetsBySpace = Math.min(context.getGasSorterProcessCount(), group.targets.size() + emptyTankCount);
        return Math.min(Math.max(group.targets.size(), maxTargetsByAmount), maxTargetsBySpace);
    }

    private List<GasDistributionPlan> buildGasDistributionPlan(GasDistributionGroup group, List<GasProcessTarget> targets) {
        GasDistributionState state = getInitialGasDistributionState(group, targets);
        List<GasDistributionPlan> plan = new ArrayList<>(targets.size());
        int remainder = state.remainder;
        for (GasProcessTarget target : targets) {
            GasDistributionTarget distributionTarget = getGasDistributionTarget(state.amountPerTank, remainder, state.minPerTank);
            plan.add(new GasDistributionPlan(target, distributionTarget.amountForTank));
            remainder = distributionTarget.remainder;
        }
        return plan;
    }

    private GasDistributionState getInitialGasDistributionState(GasDistributionGroup group, List<GasProcessTarget> targets) {
        int maxTankSize = getSmallestTargetCapacity(targets);
        int amountPerTank = group.totalAmount / targets.size();
        int remainder = group.totalAmount % targets.size();
        int minPerTank = getMinimumInput(group.gas);
        if (minPerTank > 1) {
            int perTankRemainder = amountPerTank % minPerTank;
            if (perTankRemainder > 0) {
                amountPerTank -= perTankRemainder;
                remainder += perTankRemainder * targets.size();
            }
            if (amountPerTank + minPerTank > maxTankSize) {
                minPerTank = Math.max(1, maxTankSize - amountPerTank);
            }
        }
        return new GasDistributionState(amountPerTank, remainder, minPerTank);
    }

    private int getSmallestTargetCapacity(List<GasProcessTarget> targets) {
        int capacity = Integer.MAX_VALUE;
        for (GasProcessTarget target : targets) {
            capacity = Math.min(capacity, target.tank.getCapacity());
        }
        return capacity == Integer.MAX_VALUE ? 0 : capacity;
    }

    private GasDistributionTarget getGasDistributionTarget(int amountPerTank, int remainder, int minPerTank) {
        int amountForTank = amountPerTank;
        if (remainder > 0) {
            if (remainder > minPerTank) {
                amountForTank += minPerTank;
                remainder -= minPerTank;
            } else {
                amountForTank += remainder;
                remainder = 0;
            }
        }
        return new GasDistributionTarget(amountForTank, remainder);
    }

    private boolean isGasDistributionPlanValid(GasDistributionGroup group, List<GasDistributionPlan> plan) {
        int totalAmount = 0;
        for (GasDistributionPlan target : plan) {
            int amount = target.amountForTank;
            if (amount < 0 || amount > target.target.tank.getCapacity()) {
                return false;
            }
            if (amount > 0 && !target.target.tank.isValid(new GasStack(group.gas, amount))) {
                return false;
            }
            totalAmount += amount;
        }
        return totalAmount == group.totalAmount;
    }

    private boolean applyGasDistributionPlan(GasDistributionGroup group, List<GasDistributionPlan> plan,
          List<GasProcessTarget> emptyTargets) {
        boolean changed = false;
        for (GasDistributionPlan target : plan) {
            changed |= setInputTankGas(target.target.tank, group.gas, target.amountForTank);
            if (target.amountForTank <= 0 && !emptyTargets.contains(target.target)) {
                emptyTargets.add(target.target);
            }
        }
        return changed;
    }

    private int getMinimumInput(Gas gas) {
        return Math.max(1, context.getGasSorterNeededInput(gas));
    }

    private boolean setInputTankGas(IExtendedGasTank tank, Gas gas, int amount) {
        GasStack current = tank.getGas();
        if (gas == null || amount <= 0) {
            if (current == null || current.amount <= 0) {
                return false;
            }
            tank.setStackUnchecked(null);
            return true;
        }
        if (current != null && current.getGas() == gas && current.amount == amount) {
            return false;
        }
        tank.setStackUnchecked(new GasStack(gas, amount));
        return true;
    }

    public interface Context extends IContainerTransaction {

        boolean isGasSorting();

        int getGasSorterProcessCount();

        @Nullable
        IExtendedGasTank getGasSorterInputTank(int process);

        boolean gasSorterInputProducesOutput(int process, Gas gas, boolean updateCache);

        int getGasSorterNeededInput(Gas gas);

        void onGasSorterChanged();

        default boolean isGasSorterProcessLocked(int process) {
            return false;
        }
    }

    private static class GasDistributionGroup {

        private final Gas gas;
        private final List<GasProcessTarget> targets = new ArrayList<>();
        private int totalAmount;

        private GasDistributionGroup(Gas gas) {
            this.gas = gas;
        }

        private void add(GasProcessTarget target, int amount) {
            targets.add(target);
            totalAmount += amount;
        }
    }

    private static class GasProcessTarget {

        private final int process;
        private final IExtendedGasTank tank;

        private GasProcessTarget(int process, IExtendedGasTank tank) {
            this.process = process;
            this.tank = tank;
        }
    }

    private static class GasDistributionState {

        private final int amountPerTank;
        private final int remainder;
        private final int minPerTank;

        private GasDistributionState(int amountPerTank, int remainder, int minPerTank) {
            this.amountPerTank = amountPerTank;
            this.remainder = remainder;
            this.minPerTank = minPerTank;
        }
    }

    private static class GasDistributionTarget {

        private final int amountForTank;
        private final int remainder;

        private GasDistributionTarget(int amountForTank, int remainder) {
            this.amountForTank = amountForTank;
            this.remainder = remainder;
        }
    }

    private static class GasDistributionPlan {

        private final GasProcessTarget target;
        private final int amountForTank;

        private GasDistributionPlan(GasProcessTarget target, int amountForTank) {
            this.target = target;
            this.amountForTank = amountForTank;
        }
    }
}
