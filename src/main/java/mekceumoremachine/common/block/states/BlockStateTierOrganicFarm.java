package mekceumoremachine.common.block.states;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import mekanism.common.base.IBlockType;
import mekanism.common.block.states.BlockStateFacing;
import mekanism.common.block.states.BlockStateMachine;
import mekceumoremachine.common.MEKCeuMoreMachine;
import mekceumoremachine.common.block.BlockTierOrganicFarm;
import mekceumoremachine.common.registries.MEKCeuMoreMachineBlocks;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.machine.TierOrganicFarm.*;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BlockStateTierOrganicFarm extends ExtendedBlockState {

    public BlockStateTierOrganicFarm(BlockTierOrganicFarm block, PropertyEnum<?> typeProperty) {
        super(block, new IProperty[]{BlockStateFacing.facingProperty, typeProperty, BlockStateMachine.activeProperty}, new IUnlistedProperty[]{});
    }

    public enum TierOrganicFarmMachineBlock {
        MACHINE_BLOCK;
        private PropertyEnum<MachineType> property;
        public PropertyEnum<MachineType> getProperty() {
            if (property == null) {
                property = PropertyEnum.create("type", MachineType.class, input -> input != null && input.typeBlock == this);
            }
            return property;
        }
        public Block getBlock() {
            return MEKCeuMoreMachineBlocks.TierOrganicFarm;
        }
    }

    public enum MachineType implements IStringSerializable, IBlockType {
        TIER_ORGANIC_FARM_BASIC(0, TileEntityTierOrganicFarmBasic::new, MachineTier.BASIC),
        TIER_ORGANIC_FARM_ADVANCED(1, TileEntityTierOrganicFarmAdvanced::new, MachineTier.ADVANCED),
        TIER_ORGANIC_FARM_ELITE(2, TileEntityTierOrganicFarmElite::new, MachineTier.ELITE),
        TIER_ORGANIC_FARM_ULTIMATE(3, TileEntityTierOrganicFarmUltimate::new, MachineTier.ULTIMATE);

        private static final Map<TierOrganicFarmMachineBlock, Int2ReferenceMap<MachineType>> VALID_METAS = new EnumMap<>(TierOrganicFarmMachineBlock.class);
        static { Arrays.stream(values()).forEach(type -> VALID_METAS.computeIfAbsent(type.typeBlock, ignored -> new Int2ReferenceOpenHashMap<>()).put(type.meta, type)); }
        public final TierOrganicFarmMachineBlock typeBlock = TierOrganicFarmMachineBlock.MACHINE_BLOCK;
        public final int meta;
        public final MachineTier tier;
        private final Supplier<TileEntity> tileSupplier;
        MachineType(int meta, Supplier<TileEntity> tileSupplier, MachineTier tier) { this.meta = meta; this.tileSupplier = tileSupplier; this.tier = tier; }
        public static MachineType get(Block block, int meta) { return block instanceof BlockTierOrganicFarm ? VALID_METAS.get(TierOrganicFarmMachineBlock.MACHINE_BLOCK).get(meta) : null; }
        public static MachineType get(ItemStack stack) { return get(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage()); }
        public static List<MachineType> getValidMachines() { return Arrays.asList(values()); }
        public TileEntity create() { return tileSupplier.get(); }
        public ItemStack getStack() { return new ItemStack(typeBlock.getBlock(), 1, meta); }
        public boolean isEnabled() { return true; }
        public boolean isValidMachine() { return true; }
        public boolean hasActiveTexture() { return true; }
        public boolean hasRotations() { return true; }
        public boolean canRotateTo(EnumFacing side) { return side != EnumFacing.UP && side != EnumFacing.DOWN; }
        public String getBlockName() { return "TierOrganicFarm"; }
        public String getName() { return name().toLowerCase(Locale.ROOT); }
        @Override public String toString() { return getName(); }
    }

    public static class TierOrganicFarmBlockStateMapper extends StateMapperBase {
        @Nonnull
        @Override
        protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state) {
            MachineType type = state.getValue(((BlockTierOrganicFarm) state.getBlock()).getTypeProperty());
            EnumFacing facing = state.getValue(BlockStateFacing.facingProperty);
            if (!type.canRotateTo(facing)) {
                facing = EnumFacing.NORTH;
            }
            String properties = "active=" + state.getValue(BlockStateMachine.activeProperty) + ",facing=" + facing.getName();
            return new ModelResourceLocation(new ResourceLocation(MEKCeuMoreMachine.MODID, type.getName()), properties);
        }
    }
}
