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

public class FirstRotaryCondensentratorUpgradeData extends LargeMachineUpgradeData {

    public final double clientEnergyUsed;
    public final double prevEnergy;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final GasStack gas;
    public final FluidStack fluid;
    public final boolean mode;

    public FirstRotaryCondensentratorUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source,
          double clientEnergyUsed, double prevEnergy, TileComponentConfig configComponent, TileComponentEjector ejectorComponent,
          BasicGasTank gasTank, BasicFluidTank fluidTank, int mode) {
        super(upgradeTier, source);
        this.clientEnergyUsed = clientEnergyUsed;
        this.prevEnergy = prevEnergy;
        configComponentData = new NBTTagCompound();
        configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        ejectorComponent.write(ejectorComponentData);
        gas = gasTank.getGas() == null ? null : gasTank.getGas().copy();
        fluid = fluidTank.getFluid() == null ? null : fluidTank.getFluid().copy();
        this.mode = mode == 0;
    }
}
