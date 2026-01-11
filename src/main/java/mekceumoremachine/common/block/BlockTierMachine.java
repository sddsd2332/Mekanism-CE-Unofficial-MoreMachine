package mekceumoremachine.common.block;

import mekanism.common.base.IActiveState;
import mekanism.common.block.BlockMekanismContainer;
import mekanism.common.block.states.BlockStateFacing;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.block.states.BlockStateTierMachine;
import mekceumoremachine.common.util.MEKCeuMoreMachineUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Random;

public abstract class BlockTierMachine extends BlockMekanismContainer {

    protected BlockTierMachine() {
        super(Material.IRON);
        setHardness(3.5F);
        setResistance(16F);
        setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
    }

    @Nonnull
    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateTierMachine(this);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }


    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity tile = MekanismUtils.getTileEntitySafe(worldIn, pos);
        if (tile instanceof TileEntityBasicBlock block) {
            if (block.facing != null) {
                state = state.withProperty(BlockStateFacing.facingProperty, block.facing);
            }
            state = AddActualState(state, worldIn, pos, block);
        }
        return state;
    }

    abstract IBlockState AddActualState(@Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos, TileEntity tile);

    @Override
    @Deprecated
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TileEntityBasicBlock block) {
                block.onNeighborChange(neighborBlock);
            }
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        MEKCeuMoreMachineUtils.onBlockPlacedBy(world, pos, state, placer, stack);
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        MEKCeuMoreMachineUtils.breakBlock(world, pos, state);
        super.breakBlock(world, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity) {
            if (MekanismUtils.isActive(world, pos) && tileEntity instanceof IActiveState activeState && activeState.renderUpdate() && MekanismConfig.current().client.machineEffects.val()) {
                float xRandom = (float) pos.getX() + 0.5F;
                float yRandom = (float) pos.getY() + 0.0F + random.nextFloat() * 6.0F / 16.0F;
                float zRandom = (float) pos.getZ() + 0.5F;
                float iRandom = 0.52F;
                float jRandom = random.nextFloat() * 0.6F - 0.3F;
                EnumFacing side = tileEntity.facing;
                switch (side) {
                    case WEST -> {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom - iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                        world.spawnParticle(EnumParticleTypes.REDSTONE, xRandom - iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    }
                    case EAST -> {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom + iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                        world.spawnParticle(EnumParticleTypes.REDSTONE, xRandom + iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    }
                    case NORTH -> {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom + jRandom, yRandom, zRandom - iRandom, 0.0D, 0.0D, 0.0D);
                        world.spawnParticle(EnumParticleTypes.REDSTONE, xRandom + jRandom, yRandom, zRandom - iRandom, 0.0D, 0.0D, 0.0D);
                    }
                    case SOUTH -> {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom + jRandom, yRandom, zRandom + iRandom, 0.0D, 0.0D, 0.0D);
                        world.spawnParticle(EnumParticleTypes.REDSTONE, xRandom + jRandom, yRandom, zRandom + iRandom, 0.0D, 0.0D, 0.0D);
                    }
                    default -> {
                    }
                }
            }
        }
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (MekanismConfig.current().client.enableAmbientLighting.val()) {
            TileEntity tileEntity = MekanismUtils.getTileEntitySafe(world, pos);
            if (tileEntity instanceof IActiveState activeState && activeState.lightUpdate() && activeState.wasActiveRecently()) {
                return MekanismConfig.current().client.ambientLightingLevel.val();
            }
        }
        return 0;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getBlock().getMetaFromState(state);
    }

    @Override
    public void getSubBlocks(CreativeTabs creativetabs, NonNullList<ItemStack> list) {
        MEKCeuMoreMachineUtils.getSubTierBlocks(getBlock(), list, canCharged());
    }

    public boolean canCharged() {
        return false;
    }

    abstract Block getBlock();

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(IBlockState state, @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return SecurityUtils.canAccess(player, tile) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
    }


    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        return MEKCeuMoreMachineUtils.onBlockActivated(this, getGuiID(), world, pos, state, entityplayer, hand, side, hitX, hitY, hitZ);
    }

    abstract int getGuiID();

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @Deprecated
    public boolean isFullCube(IBlockState state) {
        return false;
    }


    @Nonnull
    @Override
    @Deprecated
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }


    @SideOnly(Side.CLIENT)
    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    @Override
    protected ItemStack getDropItem(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return MEKCeuMoreMachineUtils.getDropItem(getMachineBlock(), state, world, pos);
    }

    abstract Block getMachineBlock();

    @Override
    @Deprecated
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return MEKCeuMoreMachineUtils.getComparatorInputOverride(state, world, pos);
    }

    @Override
    @Deprecated
    public boolean isSideSolid(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side) {
        return false;
    }


    @Override
    public EnumFacing[] getValidRotations(World world, @Nonnull BlockPos pos) {
        return MEKCeuMoreMachineUtils.getValidRotations(world, pos);
    }

    @Override
    public boolean rotateBlock(World world, @Nonnull BlockPos pos, @Nonnull EnumFacing axis) {
        return MEKCeuMoreMachineUtils.rotateBlock(world, pos, axis);
    }

    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return getTileEntity();
    }

    abstract TileEntity getTileEntity();

}
