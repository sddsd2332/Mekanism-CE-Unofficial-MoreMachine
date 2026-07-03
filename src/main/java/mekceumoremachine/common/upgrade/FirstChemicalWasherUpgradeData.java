package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

public class FirstChemicalWasherUpgradeData extends LargeMachineUpgradeData {

    public final double clientEnergyUsed;
    public final double prevEnergy;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final FluidStack fluid;
    public final GasStack inputGas;
    public final GasStack outputGas;

    public FirstChemicalWasherUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source, double clientEnergyUsed,
          double prevEnergy, TileComponentConfig configComponent, TileComponentEjector ejectorComponent, BasicFluidTank fluidTank,
          BasicGasTank inputTank, BasicGasTank outputTank) {
        super(upgradeTier, source);
        this.clientEnergyUsed = clientEnergyUsed;
        this.prevEnergy = prevEnergy;
        configComponentData = new NBTTagCompound();
        configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        ejectorComponent.write(ejectorComponentData);
        fluid = fluidTank.getFluid() == null ? null : fluidTank.getFluid().copy();
        inputGas = inputTank.getGas() == null ? null : inputTank.getGas().copy();
        outputGas = outputTank.getGas() == null ? null : outputTank.getGas().copy();
    }
}
