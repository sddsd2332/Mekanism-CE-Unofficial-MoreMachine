package mekceumoremachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.common.Mekanism;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tier.BaseTier;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.multiblockmachine.common.registries.MultiblockMachineBlocks;
import mekanism.multiblockmachine.common.tile.generator.TileEntityLargeWindGenerator;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.interfaces.ILargeMachine;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.common.Optional.Method;

import javax.annotation.Nonnull;
import java.util.Map;

@InterfaceList({
        @Interface(iface = "mekceumoremachine.common.tile.interfaces.ILargeMachine", modid = "mekanismmultiblockmachine"),
})
public class TileEntityTierWindGenerator extends TileEntityBaseWindGenerator implements ITierMachine<MachineTier>, ILargeMachine {

    public MachineTier tier = MachineTier.BASIC;

    public TileEntityTierWindGenerator() {
        super("TierWindGenerator");
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (isRemote()) {
            MachineTier prevTier = tier;
            tier = MachineTier.values()[dataStream.readInt()];
            if (prevTier != tier) {
                MekanismUtils.updateBlock(world, getPos());
            }
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(tier.ordinal());
        return data;
    }

    @Override
    public int processes() {
        return tier.processes;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbtTags) {
        super.readCustomNBT(nbtTags);
        tier = MachineTier.values()[nbtTags.getInteger("tier")];
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtTags) {
        super.writeCustomNBT(nbtTags);
        nbtTags.setInteger("tier", tier.ordinal());
    }

    @Override
    public boolean upgrade(BaseTier upgradeTier) {
        if (upgradeTier.ordinal() != tier.ordinal() + 1) {
            return false;
        }
        if (upgradeTier == BaseTier.CREATIVE){
            return false;
        }
        tier = MachineTier.values()[upgradeTier.ordinal()];
        Mekanism.packetHandler.sendUpdatePacket(this);
        markNoUpdateSync();
        return true;
    }

    @Override
    public MachineTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile." + fullName + "." + tier.getBaseTier().getSimpleName() + ".name");
    }

    @Override
    @Method(modid = "mekanismmultiblockmachine")
    public boolean largeMachineUpgrade(EntityPlayer player) {
        if (tier != MachineTier.ULTIMATE) {
            return false;
        }
        boolean isCanPlace = false;
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        outer:
        for (int y = 0; y <= 1; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    //跳过自己
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    testPos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        isCanPlace = true;
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.canPlace.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        break outer;
                    }
                }
            }
        }

        outer:
        for (int y = 2; y <= 43; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if ((y == 2 || y == 3 || y == 4) && x == 0 && z == 0) {
                        continue;
                    }
                    testPos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        isCanPlace = true;
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.canPlace.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        break outer;
                    }
                }
            }
        }

        outer:
        for (int y = 43; y <= 47; y++) {
            for (int z = -5; z <= 5; z++) {
                for (int x = -5; x <= 5; x++) {
                    testPos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    Block b = world.getBlockState(testPos).getBlock();
                    if (!world.isValid(testPos) || !world.isBlockLoaded(testPos, false) || !b.isReplaceable(world, testPos)) {
                        isCanPlace = true;
                        player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.canPlace.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                        break outer;
                    }
                }
            }
        }

        if (MekanismConfig.current().multiblock.LargeWindGenerationRangeStops.val()) {
            ChunkPos currentChunk = new ChunkPos(pos);
            int rangeCheck = MekanismConfig.current().multiblock.LargeWindGeneratorRangeCheck.val();
            int range;
            if (rangeCheck % 16 != 0) {
                range = rangeCheck / 16 + 1;
            } else {
                range = rangeCheck / 16;
            }
            outer:
            for (int chunkX = currentChunk.x - range; chunkX <= currentChunk.x + range; chunkX++) {
                for (int chunkZ = currentChunk.z - range; chunkZ <= currentChunk.z + range; chunkZ++) {
                    Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    Map<BlockPos, TileEntity> tileEntityMap = chunk.getTileEntityMap();
                    for (TileEntity tileEntity : tileEntityMap.values()) {
                        if (tileEntity instanceof TileEntityLargeWindGenerator) {
                            BlockPos tilePos = tileEntity.getPos();
                            double distanceSquared = pos.distanceSq(tilePos);
                            if (distanceSquared <= rangeCheck * rangeCheck) {
                                isCanPlace = true;
                                player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " " + LangUtils.localize("tooltip.tileEntity.pos") + ": " + "X " + testPos.getX() + " " + "Y " + testPos.getY() + " " + "Z " + testPos.getZ()));
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        if (isCanPlace) {
            return false;
        }

        if (world.getTileEntity(getPos()) instanceof IBoundingBlock block) {
            block.onBreak();
        } else {
            world.setBlockToAir(getPos());
        }
        world.setBlockState(getPos(), MultiblockMachineBlocks.LargeWindGenerator.getDefaultState());
        if (world.getTileEntity(getPos()) instanceof TileEntityLargeWindGenerator tile) {
            tile.onPlace();
            //Basic
            tile.facing = facing;
            tile.clientFacing = clientFacing;
            tile.ticker = ticker;
            tile.redstone = redstone;
            tile.redstoneLastTick = redstoneLastTick;
            tile.doAutoSync = doAutoSync;

            //Electric
            tile.electricityStored.set(electricityStored.get());
            tile.setAngle(getAngle());
            //Machine
            tile.setActive(isActive);
            tile.setControlType(getControlType());

            tile.securityComponent.readFrom(securityComponent);

            for (int i = 0; i < inventory.size(); i++) {
                tile.inventory.set(i, inventory.get(i));
            }
            tile.markNoUpdateSync();
            Mekanism.packetHandler.sendUpdatePacket(tile);
            markNoUpdateSync();
            return true;
        }
        return false;
    }
}
