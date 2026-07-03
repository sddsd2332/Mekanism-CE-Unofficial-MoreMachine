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
import mekceumoremachine.common.block.states.BlockStateTierChemicalCrystallizer;
import mekceumoremachine.common.block.states.BlockStateTierChemicalCrystallizer.MachineType;
import mekceumoremachine.common.block.states.BlockStateTierChemicalCrystallizer.TierChemicalCrystallizerMachineBlock;
import mekceumoremachine.common.util.MEKCeuMoreMachineUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Random;

public abstract class BlockTierChemicalCrystallizer extends BlockMekanismContainer {

    public BlockTierChemicalCrystallizer() {
        super(Material.IRON);
        setHardness(3.5F);
        setResistance(16F);
        setCreativeTab(MEKCeuMoreMachine.tabMEKCeuMoreMachine);
    }

    public static BlockTierChemicalCrystallizer getBlockMachine(TierChemicalCrystallizerMachineBlock block) {
        return new BlockTierChemicalCrystallizer() {
            @Override
            public TierChemicalCrystallizerMachineBlock getMachineBlock() {
                return block;
            }
        };
    }

    public abstract TierChemicalCrystallizerMachineBlock getMachineBlock();

    @Nonnull
    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateTierChemicalCrystallizer(this, getTypeProperty());
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta(int meta) {
        MachineType type = MachineType.get(getMachineBlock(), meta & 0xF);
        return getDefaultState().withProperty(getTypeProperty(), type);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(getTypeProperty()).meta;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity tile = MekanismUtils.getTileEntitySafe(worldIn, pos);
        if (tile instanceof TileEntityBasicBlock block && block.facing != null) {
            state = state.withProperty(BlockStateFacing.facingProperty, block.facing);
        }
        if (tile instanceof IActiveState activeState) {
            state = state.withProperty(BlockStateMachine.activeProperty, activeState.getActive());
        }
        return state;
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
        if (world.getTileEntity(pos) instanceof TileEntityBasicBlock tileEntity &&
            MekanismUtils.isActive(world, pos) && tileEntity instanceof IActiveState activeState && activeState.renderUpdate() &&
            MekanismConfig.current().client.machineEffects.val()) {
            float xRandom = (float) pos.getX() + 0.5F;
            float yRandom = (float) pos.getY() + random.nextFloat() * 6.0F / 16.0F;
            float zRandom = (float) pos.getZ() + 0.5F;
            float sideOffset = 0.52F;
            float sideRandom = random.nextFloat() * 0.6F - 0.3F;
            switch (tileEntity.facing) {
                case WEST -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom - sideOffset, yRandom, zRandom + sideRandom, 0, 0, 0);
                case EAST -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom + sideOffset, yRandom, zRandom + sideRandom, 0, 0, 0);
                case NORTH -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom + sideRandom, yRandom, zRandom - sideOffset, 0, 0, 0);
                case SOUTH -> world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, xRandom + sideRandom, yRandom, zRandom + sideOffset, 0, 0, 0);
                default -> {
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
    public void getSubBlocks(CreativeTabs creativeTabs, NonNullList<ItemStack> list) {
        for (MachineType type : MachineType.getValidMachines()) {
            if (type.typeBlock == getMachineBlock() && type.isEnabled()) {
                list.add(new ItemStack(this, 1, type.meta));
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY,
          float hitZ) {
        return MEKCeuMoreMachineUtils.onBlockActivated(this, 21, world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        int metadata = state.getBlock().getMetaFromState(state);
        MachineType type = MachineType.get(getMachineBlock(), metadata);
        return type == null ? null : type.create();
    }

    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int metadata) {
        return null;
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state) {
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

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(IBlockState state, @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return SecurityUtils.canAccess(player, tile) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
    }

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
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileEntityBasicBlock block) {
            block.onNeighborChange(neighborBlock);
        }
    }

    @Nonnull
    @Override
    protected ItemStack getDropItem(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return MEKCeuMoreMachineUtils.getDropItem(this, state, world, pos);
    }

    @Override
    @Deprecated
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @Deprecated
    public boolean isSideSolid(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side) {
        return true;
    }

    public PropertyEnum<MachineType> getTypeProperty() {
        return getMachineBlock().getProperty();
    }

    @Override
    public EnumFacing[] getValidRotations(World world, @Nonnull BlockPos pos) {
        return MEKCeuMoreMachineUtils.getValidRotations(world, pos);
    }

    @Override
    public boolean rotateBlock(World world, @Nonnull BlockPos pos, @Nonnull EnumFacing axis) {
        return MEKCeuMoreMachineUtils.rotateBlock(world, pos, axis);
    }
}
