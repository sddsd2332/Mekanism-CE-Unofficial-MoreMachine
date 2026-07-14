package mekceumoremachine.common.util;

import mekanism.common.Mekanism;
import mekanism.common.base.IFactory;
import mekanism.common.base.ITierItem;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.util.MekanismUtils;
import mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MachineStackTypeResolver {

    private static final int MAX_PRUNE_DEPTH = 32;
    private static final int MAX_PRESENTATION_CHECKS = 256;
    private static final int NORMALIZED_STACK_CACHE_SIZE = 512;
    private static final Set<Block> PICK_BLOCK_FAILURES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<String, NBTTagCompound> NORMALIZED_STACK_CACHE = Collections.synchronizedMap(
          new LinkedHashMap<String, NBTTagCompound>(NORMALIZED_STACK_CACHE_SIZE, 0.75F, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<String, NBTTagCompound> eldest) {
                  return size() > NORMALIZED_STACK_CACHE_SIZE;
              }
          });

    private MachineStackTypeResolver() {
    }

    public static MachineStackProbe probe(TileEntity tileEntity, EnumFacing facing) {
        ItemStack stack = resolveKnownTierStack(tileEntity);
        boolean cacheable = stack.isEmpty();
        if (cacheable) {
            stack = resolvePickBlockStack(tileEntity, facing);
        }
        if (stack.isEmpty()) {
            stack = resolveFallbackStack(tileEntity);
        }
        int blockMetadata = resolveBlockMetadata(tileEntity, tileEntity.getBlockType());
        ResourceLocation blockName = tileEntity.getBlockType() == null ? null : tileEntity.getBlockType().getRegistryName();
        ResourceLocation itemName = stack.isEmpty() ? null : stack.getItem().getRegistryName();
        String blockIdentity = blockName == null ? tileEntity.getClass().getName() : blockName.toString();
        String itemIdentity = itemName == null ? stack.isEmpty() ? "empty" : stack.getItem().getClass().getName() : itemName.toString();
        String baseKey = blockIdentity + '@' + blockMetadata + "/tile/" + tileEntity.getClass().getName() +
                         "/item/" + itemIdentity + '@' + (stack.isEmpty() ? 0 : stack.getItemDamage());
        String modId = blockName != null ? blockName.getNamespace() : itemName != null ? itemName.getNamespace() : "unknown";
        return new MachineStackProbe(stack, cacheable, modId, baseKey, getPresentationSignature(stack), blockMetadata);
    }

    public static ItemStack normalizeForType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack source = stack.copy();
            source.setCount(1);
            NBTTagCompound serialized = source.writeToNBT(new NBTTagCompound());
            serialized.setByte("Count", (byte) 1);
            removeStandardNonTypeTags(serialized);

            String baseline = presentationSignature(new ItemStack(serialized.copy()));
            if (baseline != null) {
                String cacheKey = serialized.getString("id") + '@' + serialized.getShort("Damage") + '\u0000' + baseline;
                NBTTagCompound cached;
                synchronized (NORMALIZED_STACK_CACHE) {
                    cached = NORMALIZED_STACK_CACHE.get(cacheKey);
                }
                if (cached != null) {
                    return new ItemStack(cached.copy());
                }
                int[] checks = {0};
                prunePayload(serialized, baseline, checks);
                synchronized (NORMALIZED_STACK_CACHE) {
                    NORMALIZED_STACK_CACHE.put(cacheKey, serialized.copy());
                }
            }

            ItemStack normalized = new ItemStack(serialized);
            if (!normalized.isEmpty()) {
                normalized.setCount(1);
                return normalized;
            }
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError fatal) {
                throw fatal;
            }
        }
        ItemStack fallback = stack.copy();
        fallback.setCount(1);
        return fallback;
    }

    public static String getTypeIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        ResourceLocation registryName = stack.getItem().getRegistryName();
        String itemName = registryName == null ? stack.getItem().getClass().getName() : registryName.toString();
        StringBuilder identity = new StringBuilder(itemName).append('@').append(stack.getItemDamage());
        try {
            NBTTagCompound payload = stack.writeToNBT(new NBTTagCompound());
            payload.removeTag("id");
            payload.removeTag("Count");
            payload.removeTag("Damage");
            if (!payload.isEmpty()) {
                identity.append('#').append(fingerprint(payload));
            }
        } catch (RuntimeException ignored) {
        }
        return identity.toString();
    }

    public static String getPresentationSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            ItemStack source = stack.copy();
            source.setCount(1);
            NBTTagCompound serialized = source.writeToNBT(new NBTTagCompound());
            serialized.setByte("Count", (byte) 1);
            removeStandardNonTypeTags(serialized);
            String signature = presentationSignature(new ItemStack(serialized));
            return signature == null ? "" : signature;
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            return "";
        }
    }

    public static void clearCache() {
        synchronized (NORMALIZED_STACK_CACHE) {
            NORMALIZED_STACK_CACHE.clear();
        }
        PICK_BLOCK_FAILURES.clear();
    }

    static String fingerprint(NBTTagCompound payload) {
        StringBuilder canonical = new StringBuilder();
        appendCanonical(canonical, payload);
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(Character.forDigit((value >>> 4) & 0xF, 16));
                result.append(Character.forDigit(value & 0xF, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash machine item data", e);
        }
    }

    private static ItemStack resolveKnownTierStack(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityFactory factory) {
            try {
                return MekanismUtils.getFactory(factory.getTier(), factory.getRecipeType());
            } catch (RuntimeException ignored) {
            }
        }

        BaseTier baseTier = resolveBaseTier(tileEntity);
        if (baseTier == null) {
            return ItemStack.EMPTY;
        }
        try {
            Block block = tileEntity.getBlockType();
            if (block == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(block, 1, resolveBlockMetadata(tileEntity, block));
            if (stack.getItem() instanceof ITierItem tierItem) {
                tierItem.setBaseTier(stack, baseTier);
                return stack;
            }
            if (tileEntity instanceof INeedRepeatTierUpgrade<?>) {
                stack.setItemDamage(baseTier.ordinal());
                return stack;
            }
        } catch (RuntimeException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack resolvePickBlockStack(TileEntity tileEntity, EnumFacing facing) {
        if (tileEntity.getWorld() == null || tileEntity.getPos() == null) {
            return ItemStack.EMPTY;
        }
        BlockPos pos = tileEntity.getPos();
        IBlockState state = tileEntity.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        try {
            EnumFacing hitSide = facing == null ? EnumFacing.UP : facing;
            Vec3d hitVec = new Vec3d(
                  pos.getX() + 0.5D + hitSide.getXOffset() * 0.5D,
                  pos.getY() + 0.5D + hitSide.getYOffset() * 0.5D,
                  pos.getZ() + 0.5D + hitSide.getZOffset() * 0.5D);
            ItemStack picked = block.getPickBlock(state, new RayTraceResult(hitVec, hitSide, pos), tileEntity.getWorld(), pos, null);
            return picked == null ? ItemStack.EMPTY : picked;
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            if (PICK_BLOCK_FAILURES.add(block)) {
                Mekanism.logger.warn("Failed to get pick block stack for wireless charging target {} at {}", block, pos, e);
            }
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack resolveFallbackStack(TileEntity tileEntity) {
        try {
            Block block = tileEntity.getBlockType();
            return block == null ? ItemStack.EMPTY : new ItemStack(block, 1, resolveBlockMetadata(tileEntity, block));
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static int resolveBlockMetadata(TileEntity tileEntity, Block block) {
        if (tileEntity.getWorld() != null && tileEntity.getPos() != null) {
            try {
                IBlockState state = tileEntity.getWorld().getBlockState(tileEntity.getPos());
                if (state.getBlock() == block) {
                    return block.damageDropped(state);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return tileEntity.getBlockMetadata();
    }

    private static BaseTier resolveBaseTier(TileEntity tileEntity) {
        if (tileEntity instanceof ITierMachine<?> tierMachine && tierMachine.getTier() != null) {
            return tierMachine.getTier().getBaseTier();
        }
        if (tileEntity instanceof INeedRepeatTierUpgrade<?> tierMachine && tierMachine.getNowTier() != null) {
            return tierMachine.getNowTier().getBaseTier();
        }
        return null;
    }

    private static void removeStandardNonTypeTags(NBTTagCompound serialized) {
        if (!serialized.hasKey("tag", 10)) {
            return;
        }
        NBTTagCompound tag = serialized.getCompoundTag("tag");
        for (String key : new String[]{"RepairCost", "ench", "StoredEnchantments", "AttributeModifiers", "CanDestroy",
              "CanPlaceOn", "HideFlags", "Unbreakable"}) {
            tag.removeTag(key);
        }
        if (tag.hasKey("display", 10)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            display.removeTag("Name");
            display.removeTag("Lore");
            if (display.isEmpty()) {
                tag.removeTag("display");
            }
        }
        if (tag.isEmpty()) {
            serialized.removeTag("tag");
        }
    }

    private static void prunePayload(NBTTagCompound serialized, String baseline, int[] checks) {
        for (String key : new String[]{"tag", "ForgeCaps"}) {
            if (!serialized.hasKey(key) || checks[0] >= MAX_PRESENTATION_CHECKS) {
                continue;
            }
            NBTBase value = serialized.getTag(key).copy();
            serialized.removeTag(key);
            if (hasPresentation(serialized, baseline, checks)) {
                continue;
            }
            serialized.setTag(key, value);
            pruneNested(serialized, value, baseline, checks, 0);
        }
    }

    private static void pruneNested(NBTTagCompound serialized, NBTBase value, String baseline, int[] checks, int depth) {
        if (depth >= MAX_PRUNE_DEPTH || checks[0] >= MAX_PRESENTATION_CHECKS) {
            return;
        }
        if (value instanceof NBTTagCompound compound) {
            List<String> keys = new ArrayList<>(compound.getKeySet());
            keys.sort(Comparator.naturalOrder());
            for (String key : keys) {
                if (!compound.hasKey(key) || checks[0] >= MAX_PRESENTATION_CHECKS) {
                    continue;
                }
                NBTBase child = compound.getTag(key).copy();
                compound.removeTag(key);
                if (hasPresentation(serialized, baseline, checks)) {
                    continue;
                }
                compound.setTag(key, child);
                pruneNested(serialized, child, baseline, checks, depth + 1);
            }
        } else if (value instanceof NBTTagList list) {
            for (int i = 0; i < list.tagCount() && checks[0] < MAX_PRESENTATION_CHECKS; i++) {
                pruneNested(serialized, list.get(i), baseline, checks, depth + 1);
            }
        }
    }

    private static boolean hasPresentation(NBTTagCompound serialized, String baseline, int[] checks) {
        checks[0]++;
        try {
            String candidate = presentationSignature(new ItemStack(serialized.copy()));
            return baseline.equals(candidate);
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            return false;
        }
    }

    private static String presentationSignature(ItemStack stack) {
        try {
            return stack.getTranslationKey() + '\u0000' + stack.getDisplayName() + '\u0000' +
                   stack.getRarity().rarityName + '\u0000' + stack.getMaxStackSize() + '\u0000' + stack.getMaxDamage();
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            return null;
        }
    }

    private static void appendCanonical(StringBuilder output, NBTBase tag) {
        output.append(tag.getId()).append(':');
        if (tag instanceof NBTTagCompound compound) {
            List<String> keys = new ArrayList<>(compound.getKeySet());
            keys.sort(Comparator.naturalOrder());
            output.append('{');
            for (String key : keys) {
                output.append(key.length()).append(':').append(key).append('=');
                appendCanonical(output, compound.getTag(key));
                output.append(';');
            }
            output.append('}');
        } else if (tag instanceof NBTTagList list) {
            output.append('[').append(list.tagCount()).append(':');
            for (NBTBase child : list) {
                appendCanonical(output, child);
                output.append(';');
            }
            output.append(']');
        } else {
            output.append(tag);
        }
    }

    public static final class MachineStackProbe {

        private final ItemStack stack;
        private final boolean cacheable;
        private final String modId;
        private final String baseKey;
        private final String presentationSignature;
        private final int blockMetadata;

        private MachineStackProbe(ItemStack stack, boolean cacheable, String modId, String baseKey, String presentationSignature,
                                  int blockMetadata) {
            this.stack = stack;
            this.cacheable = cacheable;
            this.modId = modId;
            this.baseKey = baseKey;
            this.presentationSignature = presentationSignature;
            this.blockMetadata = blockMetadata;
        }

        public ItemStack getStack() {
            return stack;
        }

        public boolean isCacheable() {
            return cacheable;
        }

        public String getModId() {
            return modId;
        }

        public String getBaseKey() {
            return baseKey;
        }

        public String getPresentationSignature() {
            return presentationSignature;
        }

        public int getBlockMetadata() {
            return blockMetadata;
        }
    }
}
