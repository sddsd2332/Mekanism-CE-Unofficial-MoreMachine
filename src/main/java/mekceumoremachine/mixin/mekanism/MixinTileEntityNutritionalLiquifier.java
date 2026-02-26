package mekceumoremachine.mixin.mekanism;

import mekanism.api.gas.GasTank;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ITierUpgradeable;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.NutritionalRecipe;
import mekanism.common.recipe.outputs.GasOutput;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import mekanism.common.tile.prefab.TileEntityBasicMachine;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tile.interfaces.ITierFirstUpgrade;
import mekceumoremachine.common.tile.machine.TierNutritional.TileEntityTierNutritionalLiquifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityNutritionalLiquifier.class, remap = false)
public abstract class MixinTileEntityNutritionalLiquifier extends TileEntityBasicMachine<ItemStackInput, GasOutput, NutritionalRecipe> implements ITierFirstUpgrade, ITierUpgradeable {

    @Shadow
    public GasTank gasTank;

    public MixinTileEntityNutritionalLiquifier(String soundPath, String name, double energyStorge, double energUsage, int upgradeSlot, int baseTicksRequired) {
        super(soundPath, name, energyStorge, energUsage, upgradeSlot, baseTicksRequired);
    }

    public boolean isUpgrade = true;

    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier != BaseTier.BASIC) {
            return false;
        }
        //看看有没有空的
        if (!inventory.get(2).isEmpty()) {
            //在机器的正上方生成
            EntityItem item = new EntityItem(world, getPos().getX(), getPos().getY() + 1, getPos().getZ(), inventory.get(2));
            item.setNoPickupDelay();
            item.isImmuneToFire = true;
            inventory.set(2, ItemStack.EMPTY);
            world.spawnEntity(item);
        }

        isUpgrade = false;
        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }
        world.setBlockState(getPos(), MEKCeuMoreMachineBlocks.TierNutritionalLiquifier.getStateFromMeta(0), 3);
        if (world.getTileEntity(getPos()) instanceof TileEntityTierNutritionalLiquifier tile) {
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Electric
            tile.electricityStored.set(electricityStored.get());
            tile.prevEnergy = prevEnergy;

            //Machine
            tile.progress[0] = operatingTicks;
            tile.setActive(isActive);
            tile.setControlType(getControlType());
            tile.prevEnergy = prevEnergy;

            tile.upgradeComponent.readFrom(upgradeComponent);
            tile.upgradeComponent.setUpgradeSlot(1);

            tile.ejectorComponent.readFrom(ejectorComponent);
            tile.ejectorComponent.setOutputData(TransmissionType.GAS, tile.configComponent.getOutputs(TransmissionType.GAS).get(1));
            tile.securityComponent.readFrom(securityComponent);
            configComponent.getTransmissions().forEach(transmission -> {
                tile.configComponent.setConfig(transmission, configComponent.getConfig(transmission).asByteArray());
                tile.configComponent.setEjecting(transmission, configComponent.isEjecting(transmission));
            });
            tile.inventory.set(0, inventory.get(1));
            tile.inventory.set(2, inventory.get(0));
            tile.inventory.set(1, inventory.get(3));

            tile.outputTank1.setGas(gasTank.getGas());
            tile.upgradeComponent.getSupportedTypes().forEach(tile::recalculateUpgradables);
            tile.upgraded = true;
            tile.isUpgrade = true;
            tile.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(tile);
            markNoUpdateSync();
            return true;

        }
        return false;
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
