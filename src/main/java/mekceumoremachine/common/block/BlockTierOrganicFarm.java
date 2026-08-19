package mekceumoremachine.common.block;

import mekanism.common.base.IActiveState;
import mekanism.common.block.BlockMekanismContainer;
import mekanism.common.block.states.BlockStateFacing;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.block.states.BlockStateTierOrganicFarm;
import mekceumoremachine.common.block.states.BlockStateTierOrganicFarm.MachineType;
import mekceumoremachine.common.block.states.BlockStateTierOrganicFarm.TierOrganicFarmMachineBlock;
import mekceumoremachine.common.util.MEKCeuMoreMachineUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.PropertyEnum;
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

public class BlockTierOrganicFarm extends BlockMekanismContainer {
    public BlockTierOrganicFarm() {
        super(Material.IRON);
        setHardness(3.5F);
        setResistance(16F);
        setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
    }
    public TierOrganicFarmMachineBlock getMachineBlock() { return TierOrganicFarmMachineBlock.MACHINE_BLOCK; }
    public PropertyEnum<MachineType> getTypeProperty() { return getMachineBlock().getProperty(); }
    @Nonnull @Override public BlockStateContainer createBlockState() { return new BlockStateTierOrganicFarm(this, getTypeProperty()); }
    @Nonnull @Override @Deprecated public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(getTypeProperty(), MachineType.get(this, meta & 15)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(getTypeProperty()).meta; }
    @Nonnull @Override @Deprecated public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = MekanismUtils.getTileEntitySafe(world, pos);
        if (tile instanceof TileEntityBasicBlock block && block.facing != null) state = state.withProperty(BlockStateFacing.facingProperty, block.facing);
        if (tile instanceof IActiveState active) state = state.withProperty(BlockStateMachine.activeProperty, active.getActive());
        return state;
    }
    @Override @SideOnly(Side.CLIENT) public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity && MekanismUtils.isActive(world, pos) &&
              tileEntity instanceof IActiveState activeState && activeState.renderUpdate() &&
              MekanismConfig.current().client.machineEffects.val()) {
            float x = pos.getX() + 0.5F;
            float y = pos.getY() + random.nextFloat() * 6F / 16F;
            float z = pos.getZ() + 0.5F;
            float sideOffset = 0.52F;
            float sideRandom = random.nextFloat() * 0.6F - 0.3F;
            switch (tileEntity.facing) {
                case WEST -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x - sideOffset, y, z + sideRandom, 0, 0, 0);
                case EAST -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + sideOffset, y, z + sideRandom, 0, 0, 0);
                case NORTH -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + sideRandom, y, z - sideOffset, 0, 0, 0);
                case SOUTH -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + sideRandom, y, z + sideOffset, 0, 0, 0);
                default -> {
                }
            }
        }
    }
    @Override public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (MekanismConfig.current().client.enableAmbientLighting.val()) {
            TileEntity tile = MekanismUtils.getTileEntitySafe(world, pos);
            if (tile instanceof IActiveState active && active.lightUpdate() && active.wasActiveRecently()) {
                return MekanismConfig.current().client.ambientLightingLevel.val();
            }
        }
        return 0;
    }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) { MEKCeuMoreMachineUtils.onBlockPlacedBy(world, pos, state, placer, stack); }
    @Override public void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) { MEKCeuMoreMachineUtils.breakBlock(world, pos, state); super.breakBlock(world, pos, state); }
    @Override public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) { for (MachineType type : MachineType.values()) list.add(type.getStack()); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) { return MEKCeuMoreMachineUtils.onBlockActivated(this, 22, world, pos, state, player, hand, side, hitX, hitY, hitZ); }
    @Override public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) { MachineType type = state.getValue(getTypeProperty()); return type == null ? null : type.create(); }
    @Override public TileEntity createNewTileEntity(@Nonnull World world, int metadata) { return null; }
    @Override public int damageDropped(IBlockState state) { return getMetaFromState(state); }
    @Nonnull @Override protected ItemStack getDropItem(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) { return MEKCeuMoreMachineUtils.getDropItem(this, state, world, pos); }
    @Override @Deprecated public float getPlayerRelativeBlockHardness(IBlockState state, @Nonnull EntityPlayer player,
          @Nonnull World world, @Nonnull BlockPos pos) {
        return SecurityUtils.canAccess(player, world.getTileEntity(pos)) ?
              super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0;
    }
    @Override @Deprecated public boolean hasComparatorInputOverride(IBlockState state) { return true; }
    @Override @Deprecated public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return MEKCeuMoreMachineUtils.getComparatorInputOverride(state, world, pos);
    }
    @Override @Deprecated public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock,
          BlockPos neighborPos) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileEntityBasicBlock tile) {
            tile.onNeighborChange(neighborBlock);
        }
    }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Nonnull @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }
    @Nonnull @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
}
