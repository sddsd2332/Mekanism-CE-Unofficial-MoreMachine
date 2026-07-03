package mekceumoremachine.common.upgrade;

import mekanism.api.gas.GasStack;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityContainerBlock;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class FirstIsotopicCentrifugeUpgradeData extends LargeMachineUpgradeData {

    public final double clientEnergyUsed;
    public final double prevEnergy;
    public final int operatingTicks;
    public final NBTTagCompound configComponentData;
    public final NBTTagCompound ejectorComponentData;
    public final GasStack inputGas;
    public final GasStack outputGas;

    public FirstIsotopicCentrifugeUpgradeData(@Nonnull BaseTier upgradeTier, @Nonnull TileEntityContainerBlock source,
          double clientEnergyUsed, double prevEnergy, int operatingTicks, TileComponentConfig configComponent,
          TileComponentEjector ejectorComponent, BasicGasTank inputTank, BasicGasTank outputTank) {
        super(upgradeTier, source);
        this.clientEnergyUsed = clientEnergyUsed;
        this.prevEnergy = prevEnergy;
        this.operatingTicks = operatingTicks;
        configComponentData = new NBTTagCompound();
        configComponent.write(configComponentData);
        ejectorComponentData = new NBTTagCompound();
        ejectorComponent.write(ejectorComponentData);
        inputGas = inputTank.getGas() == null ? null : inputTank.getGas().copy();
        outputGas = outputTank.getGas() == null ? null : outputTank.getGas().copy();
    }
}
