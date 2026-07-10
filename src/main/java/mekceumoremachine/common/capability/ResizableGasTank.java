package mekceumoremachine.common.capability;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.IContentsListenerRegistry;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.IExtendedGasTank;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ResizableGasTank extends GasTank implements IExtendedGasTank, IContentsListenerRegistry {

    public static final Predicate<GasStack> ALWAYS_TRUE = ConstantPredicates.alwaysTrue();
    public static final BiPredicate<GasStack, AutomationType> ALWAYS_TRUE_BI = ConstantPredicates.alwaysTrueBi();
    public static final BiPredicate<GasStack, AutomationType> INTERNAL_ONLY = ConstantPredicates.internalOnly();
    public static final BiPredicate<GasStack, AutomationType> NOT_EXTERNAL = ConstantPredicates.notExternal();

    private final BiPredicate<GasStack, AutomationType> canExtract;
    private final BiPredicate<GasStack, AutomationType> canInsert;
    private final Predicate<GasStack> validator;
    @Nullable
    private final IContentsListener listener;
    @Nullable
    private volatile IContentsListener[] additionalListeners;

    public ResizableGasTank(int capacity) {
        this(capacity, ALWAYS_TRUE_BI, ALWAYS_TRUE_BI, ALWAYS_TRUE, null);
    }

    public static ResizableGasTank input(int capacity, Predicate<Gas> validator, @Nullable IContentsListener listener) {
        return new ResizableGasTank(capacity, NOT_EXTERNAL, ALWAYS_TRUE_BI, wrap(validator), listener);
    }

    public static ResizableGasTank output(int capacity, @Nullable IContentsListener listener) {
        return new ResizableGasTank(capacity, ALWAYS_TRUE_BI, INTERNAL_ONLY, ALWAYS_TRUE, listener);
    }

    public static ResizableGasTank create(int capacity, Predicate<Gas> validator, @Nullable IContentsListener listener) {
        return new ResizableGasTank(capacity, ALWAYS_TRUE_BI, ALWAYS_TRUE_BI, wrap(validator), listener);
    }

    public ResizableGasTank(int capacity, BiPredicate<GasStack, AutomationType> canExtract, BiPredicate<GasStack, AutomationType> canInsert,
          Predicate<GasStack> validator, @Nullable IContentsListener listener) {
        super(capacity);
        this.canExtract = Objects.requireNonNull(canExtract, "Extraction predicate cannot be null");
        this.canInsert = Objects.requireNonNull(canInsert, "Insertion predicate cannot be null");
        this.validator = Objects.requireNonNull(validator, "Gas validator cannot be null");
        this.listener = listener;
    }

    @Override
    public void setStack(@Nullable GasStack stack) {
        if (stack != null && !isValid(stack)) {
            throw new RuntimeException("Invalid gas for tank: " + stack);
        }
        setStackUnchecked(stack);
    }

    @Override
    public void setStackUnchecked(@Nullable GasStack stack) {
        setGas(stack);
    }

    @Override
    public void setGas(@Nullable GasStack stack) {
        super.setGas(stack);
        onContentsChanged();
    }

    @Override
    public void read(NBTTagCompound nbtTags) {
        int capacity = getMaxGas();
        super.read(nbtTags);
        setMaxGas(capacity);
        GasStack stored = getGas();
        if (stored != null && (stored.amount <= 0 || stored.getGas() == null)) {
            setGas(null);
        } else if (stored != null && stored.amount > capacity) {
            stored.amount = capacity;
            onContentsChanged();
        } else {
            onContentsChanged();
        }
    }

    @Override
    public boolean isValid(@Nullable GasStack stack) {
        return stack != null && stack.getGas() != null && validator.test(stack);
    }

    @Override
    @Nullable
    public GasStack insert(@Nullable GasStack stack, Action action, AutomationType automationType) {
        if (stack == null || stack.amount <= 0 || !isValid(stack) || !canInsert.test(stack, automationType)) {
            return stack;
        }
        int needed = getNeeded();
        if (needed <= 0) {
            return stack;
        }
        GasStack stored = getGas();
        boolean sameType = false;
        if (stored == null || (sameType = stored.isGasEqual(stack))) {
            int toAdd = Math.min(stack.amount, needed);
            if (action.execute()) {
                if (sameType) {
                    stored.amount += toAdd;
                    onContentsChanged();
                } else {
                    setStackUnchecked(new GasStack(stack.getGas(), toAdd));
                }
            }
            return stack.amount == toAdd ? null : new GasStack(stack.getGas(), stack.amount - toAdd);
        }
        return stack;
    }

    @Override
    @Nullable
    public GasStack extract(int amount, Action action, AutomationType automationType) {
        GasStack stored = getGas();
        if (stored == null || amount <= 0 || !canExtract.test(stored, automationType)) {
            return null;
        }
        GasStack extracted = new GasStack(stored.getGas(), Math.min(getGasAmount(), amount));
        if (extracted.amount <= 0) {
            return null;
        }
        if (action.execute()) {
            stored.amount -= extracted.amount;
            if (stored.amount <= 0) {
                setStackUnchecked(null);
            } else {
                onContentsChanged();
            }
        }
        return extracted;
    }

    @Override
    public void onContentsChanged() {
        if (listener != null) {
            listener.onContentsChanged();
        }
        IContentsListener[] listeners = additionalListeners;
        if (listeners != null) {
            for (IContentsListener additionalListener : listeners) {
                additionalListener.onContentsChanged();
            }
        }
    }

    @Override
    public synchronized boolean addContentsListener(IContentsListener listener) {
        if (listener == null || listener == this || listener == this.listener) {
            return false;
        }
        IContentsListener[] listeners = additionalListeners;
        if (listeners == null) {
            additionalListeners = new IContentsListener[]{listener};
            return true;
        }
        for (IContentsListener existing : listeners) {
            if (existing == listener) {
                return false;
            }
        }
        IContentsListener[] updated = new IContentsListener[listeners.length + 1];
        System.arraycopy(listeners, 0, updated, 0, listeners.length);
        updated[listeners.length] = listener;
        additionalListeners = updated;
        return true;
    }

    @Override
    public synchronized boolean removeContentsListener(IContentsListener listener) {
        if (listener == null) {
            return false;
        }
        IContentsListener[] listeners = additionalListeners;
        if (listeners == null) {
            return false;
        }
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == listener) {
                if (listeners.length == 1) {
                    additionalListeners = null;
                } else {
                    IContentsListener[] updated = new IContentsListener[listeners.length - 1];
                    System.arraycopy(listeners, 0, updated, 0, i);
                    System.arraycopy(listeners, i + 1, updated, i, listeners.length - i - 1);
                    additionalListeners = updated;
                }
                return true;
            }
        }
        return false;
    }

    private static Predicate<GasStack> wrap(Predicate<Gas> validator) {
        Objects.requireNonNull(validator, "Gas validator cannot be null");
        return stack -> stack != null && stack.getGas() != null && validator.test(stack.getGas());
    }
}
