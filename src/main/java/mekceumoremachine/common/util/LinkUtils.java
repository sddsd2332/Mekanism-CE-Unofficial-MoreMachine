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

    public static boolean isValidAcceptorOnSideInput(TileEntity tile, EnumFacing side) {
        if (tile == null || CableUtils.isCable(tile)) {
            return false;
        }
        return isAcceptor(tile, side) ||
                (MekanismUtils.useRF() && MoreMachineConfig.current().config.enableRFWirelessRecharge.val() && tile instanceof IEnergyConnection connection && connection.canConnectEnergy(side.getOpposite())) ||
                (MekanismUtils.useForge() && MoreMachineConfig.current().config.enableFEWirelessRecharge.val() && CapabilityUtils.hasCapability(tile, CapabilityEnergy.ENERGY, side.getOpposite()));
    }

    private static boolean isAcceptor(TileEntity tileEntity, EnumFacing side) {
        if (CapabilityUtils.hasCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, side.getOpposite())) {
            return false;
        }
        IStrictEnergyAcceptor strictEnergyAcceptor;
        if ((strictEnergyAcceptor = CapabilityUtils.getCapability(tileEntity, Capabilities.ENERGY_ACCEPTOR_CAPABILITY, side.getOpposite())) != null) {
            return strictEnergyAcceptor.canReceiveEnergy(side.getOpposite());
        }
        if (MekanismUtils.useTesla() && MoreMachineConfig.current().config.enableTeslaWirelessRecharge.val() && CapabilityUtils.hasCapability(tileEntity, Capabilities.TESLA_CONSUMER_CAPABILITY, side.getOpposite())) {
            return true;
        }
        IEnergyStorage energyStorage;
        if (MekanismUtils.useForge() && MoreMachineConfig.current().config.enableFEWirelessRecharge.val() && (energyStorage = CapabilityUtils.getCapability(tileEntity, CapabilityEnergy.ENERGY, side.getOpposite())) != null) {
            return energyStorage.canReceive();
        }
        if (MekanismUtils.useRF() && MoreMachineConfig.current().config.enableRFWirelessRecharge.val() && tileEntity instanceof IEnergyReceiver receiver) {
            return receiver.canConnectEnergy(side.getOpposite());
        }
        return MekanismUtils.useIC2() && MoreMachineConfig.current().config.enableEuWirelessRecharge.val() && IC2Integration.isAcceptor(tileEntity, side);
    }
}
