package mekceumoremachine.common.util;

import cofh.redstoneflux.api.IEnergyConnection;
import cofh.redstoneflux.api.IEnergyReceiver;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.integration.ic2.IC2Integration;
import mekanism.common.util.CableUtils;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.config.MoreMachineConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public final class LinkUtils {

    public static boolean isValidAcceptorOnTargetSideInput(TileEntity tile, EnumFacing targetSide) {
        if (tile == null || CableUtils.isCable(tile)) {
            return false;
        }
        return isAcceptor(tile, targetSide) ||
                (MekanismUtils.useRF() && MoreMachineConfig.current().config.enableRFWirelessRecharge.val() && tile instanceof IEnergyConnection connection && connection.canConnectEnergy(targetSide)) ||
                (MekanismUtils.useForge() && MoreMachineConfig.current().config.enableFEWirelessRecharge.val() && CapabilityUtils.hasCapability(tile, CapabilityEnergy.ENERGY, targetSide));
    }

    private static boolean isAcceptor(TileEntity tileEntity, EnumFacing targetSide) {
        if (CapabilityUtils.hasCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, targetSide)) {
            return false;
        }
        IStrictEnergyAcceptor strictEnergyAcceptor;
        if ((strictEnergyAcceptor = CapabilityUtils.getCapability(tileEntity, Capabilities.ENERGY_ACCEPTOR_CAPABILITY, targetSide)) != null) {
            return strictEnergyAcceptor.canReceiveEnergy(targetSide);
        }
        if (MekanismUtils.useTesla() && MoreMachineConfig.current().config.enableTeslaWirelessRecharge.val() && CapabilityUtils.hasCapability(tileEntity, Capabilities.TESLA_CONSUMER_CAPABILITY, targetSide)) {
            return true;
        }
        IEnergyStorage energyStorage;
        if (MekanismUtils.useForge() && MoreMachineConfig.current().config.enableFEWirelessRecharge.val() && (energyStorage = CapabilityUtils.getCapability(tileEntity, CapabilityEnergy.ENERGY, targetSide)) != null) {
            return energyStorage.canReceive();
        }
        if (MekanismUtils.useRF() && MoreMachineConfig.current().config.enableRFWirelessRecharge.val() && tileEntity instanceof IEnergyReceiver receiver) {
            return receiver.canConnectEnergy(targetSide);
        }
        return MekanismUtils.useIC2() && MoreMachineConfig.current().config.enableEuWirelessRecharge.val() && IC2Integration.isAcceptor(tileEntity, targetSide.getOpposite());
    }
}
