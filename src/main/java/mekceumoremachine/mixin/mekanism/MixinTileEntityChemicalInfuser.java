package mekceumoremachine.mixin.mekanism;

import mekanism.common.capabilities.gas.BasicGasTank;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.recipe.inputs.ChemicalPairInput;
import mekanism.common.recipe.machines.ChemicalInfuserRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityChemicalInfuser;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.upgrade.FirstChemicalInfuserUpgradeData;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TileEntityChemicalInfuser.class, remap = false)
public abstract class MixinTileEntityChemicalInfuser extends TileEntityBasicMachine<ChemicalPairInput, GasOutput, ChemicalInfuserRecipe> implements ITierFirstUpgrade {

    @Shadow
    public double clientEnergyUsed;

    @Shadow
    public BasicGasTank centerTank;

    @Shadow
    public BasicGasTank leftTank;

    @Shadow
    public BasicGasTank rightTank;

    public MixinTileEntityChemicalInfuser(String soundPath, BlockStateMachine.MachineType type, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, type, upgradeSlot, baseTicksRequired);
    }

    @Unique
    public boolean isUpgrade = true;

    @Override
    public IBlockState getUpgradeResult(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier) ? MEKCeuMoreMachineBlocks.TierChemicalInfuser.getDefaultState() : null;
    }

    @Override
    public void prepareForUpgrade() {
        isUpgrade = false;
    }

    @Override
    public IUpgradeData getUpgradeData(BaseTier upgradeTier) {
        return canInstallUpgrade(upgradeTier)
              ? new FirstChemicalInfuserUpgradeData(upgradeTier, this, clientEnergyUsed, prevEnergy, configComponent, ejectorComponent,
                    leftTank, rightTank, centerTank)
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
