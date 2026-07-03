package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.recipe.inputs.FluidInput;
import mekanism.common.recipe.machines.SeparatorRecipe;
import mekanism.common.recipe.outputs.ChemicalPairOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityGasTank;
import mekanism.common.tile.machine.TileEntityElectrolyticSeparator;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstElectrolyticSeparatorUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityElectrolyticSeparator.class, remap = false)
public abstract class MixinTileEntityElectrolyticSeparator extends TileEntityBasicMachine<FluidInput, ChemicalPairOutput, SeparatorRecipe> implements ITierFirstUpgrade {

    @Shadow
    public BasicGasTank rightTank;

    @Shadow
    public BasicGasTank leftTank;

    @Shadow
    public BasicFluidTank fluidTank;

    @Shadow
    public TileEntityGasTank.GasMode dumpLeft;

    @Shadow
    public TileEntityGasTank.GasMode dumpRight;

    @Shadow
    public double clientEnergyUsed;


    public MixinTileEntityElectrolyticSeparator(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }

    public boolean isUpgrade = true;


    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierElectrolyticSeparator.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstElectrolyticSeparatorUpgradeData(upgradeTier, this, clientEnergyUsed, prevEnergy, configComponent, ejectorComponent,
                    fluidTank, leftTank, rightTank, dumpLeft, dumpRight)
              : null;
    }

    /**
     * @author sddsd2332
     * @reason 取消在升级的时候进行辐射判断
     */
    @Override
    public boolean shouldDumpRadiation() {
        return isUpgrade;
    }
}
