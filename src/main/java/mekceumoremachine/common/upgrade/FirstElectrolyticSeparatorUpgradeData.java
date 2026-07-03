package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityGasTank.GasMode;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

public class FirstElectrolyticSeparatorUpgradeData extends LargeMachineUpgradeData {

    public final double clientEnergyUsed;
    public final double prevEnergy;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final FluidStack fluid;
    public final GasStack leftGas;
    public final GasStack rightGas;
    public final GasMode dumpLeft;
    public final GasMode dumpRight;

    public FirstElectrolyticSeparatorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source,
          double clientEnergyUsed, double prevEnergy, TileComponentConfig configComponent, TileComponentEjector ejectorComponent,
          BasicFluidTank fluidTank, BasicGasTank leftTank, BasicGasTank rightTank, GasMode dumpLeft, GasMode dumpRight) {
        super(upgradeTier, source);
        this.clientEnergyUsed = clientEnergyUsed;
        this.prevEnergy = prevEnergy;
        configComponentData = new NBTTagCompound();
        configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        ejectorComponent.write(ejectorComponentData);
        fluid = fluidTank.getFluid() == null ? null : fluidTank.getFluid().copy();
        leftGas = leftTank.getGas() == null ? null : leftTank.getGas().copy();
        rightGas = rightTank.getGas() == null ? null : rightTank.getGas().copy();
        this.dumpLeft = dumpLeft;
        this.dumpRight = dumpRight;
    }
}
