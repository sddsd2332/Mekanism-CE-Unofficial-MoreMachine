package mekceumoremachine.common.capability;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.functions.ConstantPredicates;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ResizableFluidTank extends FluidTank implements IExtendedFluidTank {

    public static final Predicate<FluidStack> ALWAYS_TRUE = ConstantPredicates.alwaysTrue();
    public static final BiPredicate<FluidStack, AutomationType> ALWAYS_TRUE_BI = ConstantPredicates.alwaysTrueBi();
    public static final BiPredicate<FluidStack, AutomationType> INTERNAL_ONLY = ConstantPredicates.internalOnly();
    public static final BiPredicate<FluidStack, AutomationType> NOT_EXTERNAL = ConstantPredicates.notExternal();

    private final BiPredicate<FluidStack, AutomationType> canExtract;
    private final BiPredicate<FluidStack, AutomationType> canInsert;
    private final Predicate<FluidStack> validator;
    @Nullable
    private final IContentsListener listener;

    public ResizableFluidTank(int capacity) {
        this(capacity, ALWAYS_TRUE_BI, ALWAYS_TRUE_BI, ALWAYS_TRUE, null);
    }

    public static ResizableFluidTank input(int capacity, Predicate<FluidStack> validator, @Nullable IContentsListener listener) {
        return new ResizableFluidTank(capacity, NOT_EXTERNAL, ALWAYS_TRUE_BI, validator, listener);
    }

    public static ResizableFluidTank output(int capacity, @Nullable IContentsListener listener) {
        return new ResizableFluidTank(capacity, ALWAYS_TRUE_BI, INTERNAL_ONLY, ALWAYS_TRUE, listener);
    }

    public static ResizableFluidTank create(int capacity, Predicate<FluidStack> validator, @Nullable IContentsListener listener) {
        return new ResizableFluidTank(capacity, ALWAYS_TRUE_BI, ALWAYS_TRUE_BI, validator, listener);
    }

    public ResizableFluidTank(int capacity, BiPredicate<FluidStack, AutomationType> canExtract, BiPredicate<FluidStack, AutomationType> canInsert,
          Predicate<FluidStack> validator, @Nullable IContentsListener listener) {
        super(capacity);
        this.canExtract = Objects.requireNonNull(canExtract, "Extraction predicate cannot be null");
        this.canInsert = Objects.requireNonNull(canInsert, "Insertion predicate cannot be null");
        this.validator = Objects.requireNonNull(validator, "Fluid validator cannot be null");
        this.listener = listener;
    }

    @Override
    public void setStack(@Nullable FluidStack stack) {
        if (stack != null && !isFluidValid(stack)) {
            throw new RuntimeException("Invalid fluid for tank: " + stack);
        }
        setStackUnchecked(stack);
    }

    @Override
    public void setStackUnchecked(@Nullable FluidStack stack) {
        setFluid(stack);
    }

    @Override
    public void setFluid(@Nullable FluidStack stack) {
        super.setFluid(stack);
        onContentsChanged();
    }

    @Override
    public FluidTank readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        FluidStack stored = getFluid();
        if (stored != null && (stored.amount <= 0 || stored.getFluid() == null)) {
            setFluid(null);
        } else if (stored != null && stored.amount > getCapacity()) {
            stored.amount = getCapacity();
            onContentsChanged();
        }
        return this;
    }

    @Override
    public boolean isFluidValid(@Nullable FluidStack stack) {
        return stack != null && validator.test(stack);
    }

    @Override
    @Nullable
    public FluidStack insert(@Nullable FluidStack stack, Action action, AutomationType automationType) {
        if (stack == null || stack.amount <= 0 || !isFluidValid(stack) || !canInsert.test(stack, automationType)) {
            return stack;
        }
        int needed = getNeeded();
        if (needed <= 0) {
            return stack;
        }
        FluidStack stored = getFluid();
        boolean sameType = false;
        if (stored == null || (sameType = stored.isFluidEqual(stack))) {
            int toAdd = Math.min(stack.amount, needed);
            if (action.execute()) {
                if (sameType) {
                    stored.amount += toAdd;
                    onContentsChanged();
                } else {
                    setStackUnchecked(new FluidStack(stack, toAdd));
                }
            }
            return stack.amount == toAdd ? null : new FluidStack(stack, stack.amount - toAdd);
        }
        return stack;
    }

    @Override
    @Nullable
    public FluidStack extract(int amount, Action action, AutomationType automationType) {
        FluidStack stored = getFluid();
        if (stored == null || amount <= 0 || !canExtract.test(stored, automationType)) {
            return null;
        }
        FluidStack extracted = new FluidStack(stored, Math.min(getFluidAmount(), amount));
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
    }
}
