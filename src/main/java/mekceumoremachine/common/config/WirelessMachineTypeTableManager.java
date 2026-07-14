package mekceumoremachine.common.config;

import mekanism.common.Mekanism;
import mekanism.common.base.IFactory;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.factory.TileEntityFactory;
import mekceumoremachine.common.attachments.component.MachineTypeDescriptor;
import mekceumoremachine.common.tile.interfaces.INeedRepeatTierUpgrade;
import mekceumoremachine.common.tile.interfaces.ITierMachine;
import mekceumoremachine.common.util.MachineStackTypeResolver;
import mekceumoremachine.common.util.MachineStackTypeResolver.MachineStackProbe;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WirelessMachineTypeTableManager {

    private static final int DATA_VERSION = 1;
    private static final int RESOLVER_VERSION = 1;
    private static final String UNKNOWN_MOD_ID = "unknown";
    private static final String TABLE_FILE_NAME = "machine_types.dat";
    private static final Map<String, MachineTypeTable> TABLES = new HashMap<>();

    private WirelessMachineTypeTableManager() {
    }

    public static MachineTypeDescriptor resolve(TileEntity tileEntity, EnumFacing facing) {
        MachineStackProbe probe = MachineStackTypeResolver.probe(tileEntity, facing);
        MachineTypeTable table = probe.isCacheable() ? getTable(tileEntity.getWorld()) : null;
        if (table != null && !probe.getPresentationSignature().isEmpty()) {
            MachineTypeDescriptor cached = table.find(probe.getModId(), probe.getBaseKey(), probe.getPresentationSignature());
            if (cached != null) {
                return cached;
            }
        }

        ItemStack machineStack = MachineStackTypeResolver.normalizeForType(probe.getStack());
        MachineTypeDescriptor descriptor = createDescriptor(tileEntity, probe.getBlockMetadata(), machineStack);
        if (table != null && !probe.getPresentationSignature().isEmpty() &&
            table.put(probe.getModId(), probe.getBaseKey(), probe.getPresentationSignature(), descriptor)) {
            table.save();
        }
        return descriptor;
    }

    public static File getConnectionsDirectory(World world) {
        if (world == null || world.isRemote || world.getSaveHandler() == null) {
            return null;
        }
        return getConnectionsDirectory(world.getSaveHandler().getWorldDirectory());
    }

    static File getConnectionsDirectory(File worldDirectory) {
        File dataDirectory = new File(worldDirectory, "data");
        return new File(new File(new File(new File(dataDirectory, "mek"), "moremachine"), "wireless_energy"), "connections");
    }

    public static void clearAll() {
        synchronized (TABLES) {
            TABLES.clear();
        }
        MachineStackTypeResolver.clearCache();
    }

    private static MachineTypeTable getTable(World world) {
        File directory = getConnectionsDirectory(world);
        if (directory == null) {
            return null;
        }
        String worldPath = world.getSaveHandler().getWorldDirectory().getAbsolutePath();
        synchronized (TABLES) {
            MachineTypeTable table = TABLES.get(worldPath);
            if (table == null) {
                table = new MachineTypeTable(new File(directory, TABLE_FILE_NAME));
                table.load();
                TABLES.put(worldPath, table);
            }
            return table;
        }
    }

    private static MachineTypeDescriptor createDescriptor(TileEntity tileEntity, int metadata, ItemStack machineStack) {
        String tileClassName = tileEntity.getClass().getName();
        String machineTypeKey;
        try {
            Block block = tileEntity.getBlockType();
            ResourceLocation registryName = block == null ? null : block.getRegistryName();
            StringBuilder key = new StringBuilder(registryName == null ? tileClassName : registryName.toString());
            MachineType machineType = block == null ? null : MachineType.get(block, metadata);
            if (machineType != null) {
                key.append("/machine/").append(machineType.getName());
            } else {
                key.append("/meta/").append(metadata);
            }
            if (tileEntity instanceof TileEntityFactory factory) {
                IFactory.RecipeType recipeType = factory.getRecipeType();
                if (recipeType != null) {
                    key.append("/recipe/").append(recipeType.getName());
                }
            }
            BaseTier baseTier = resolveBaseTier(tileEntity);
            if (baseTier != null) {
                key.append("/tier/").append(baseTier.getSimpleName());
            }
            if (!machineStack.isEmpty()) {
                key.append("/stack/").append(MachineStackTypeResolver.getTypeIdentity(machineStack));
            }
            key.append("/tile/").append(tileClassName);
            machineTypeKey = key.toString();
        } catch (RuntimeException ignored) {
            machineTypeKey = "tile/" + tileClassName;
        }
        return new MachineTypeDescriptor(machineTypeKey, resolveMachineNameKey(tileEntity, machineStack), machineStack);
    }

    private static String resolveMachineNameKey(TileEntity tileEntity, ItemStack machineStack) {
        if (!machineStack.isEmpty()) {
            try {
                String translationKey = machineStack.getTranslationKey();
                if (translationKey != null && !translationKey.isEmpty() && !"null".equals(translationKey)) {
                    return translationKey;
                }
            } catch (RuntimeException ignored) {
            }
        }
        try {
            Block block = tileEntity.getBlockType();
            if (block != null && block.getTranslationKey() != null && !block.getTranslationKey().isEmpty()) {
                return block.getTranslationKey();
            }
        } catch (RuntimeException ignored) {
        }
        return tileEntity.getClass().getSimpleName();
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

    private static final class MachineTypeTable {

        private final File file;
        private final Map<String, Map<String, List<CachedVariant>>> entries = new LinkedHashMap<>();

        private MachineTypeTable(File file) {
            this.file = file;
        }

        private synchronized void load() {
            entries.clear();
            if (!file.isFile()) {
                return;
            }
            try {
                NBTTagCompound root = CompressedStreamTools.read(file);
                if (root.getInteger("version") != DATA_VERSION || root.getInteger("resolverVersion") != RESOLVER_VERSION) {
                    Mekanism.logger.warn("Unsupported wireless machine type table version in {}", file);
                    return;
                }
                boolean needsRewrite = false;
                NBTTagList mods = root.getTagList("mods", NBT.TAG_COMPOUND);
                for (int modIndex = 0; modIndex < mods.tagCount(); modIndex++) {
                    NBTTagCompound modTag = mods.getCompoundTagAt(modIndex);
                    needsRewrite |= modTag.hasKey("modVersion");
                    String modId = normalizeModId(modTag.getString("modId"));
                    NBTTagList machines = modTag.getTagList("machines", NBT.TAG_COMPOUND);
                    for (int machineIndex = 0; machineIndex < machines.tagCount(); machineIndex++) {
                        NBTTagCompound machineTag = machines.getCompoundTagAt(machineIndex);
                        String baseKey = machineTag.getString("baseKey");
                        if (baseKey.isEmpty()) {
                            continue;
                        }
                        NBTTagList variants = machineTag.getTagList("variants", NBT.TAG_COMPOUND);
                        for (int variantIndex = 0; variantIndex < variants.tagCount(); variantIndex++) {
                            NBTTagCompound variantTag = variants.getCompoundTagAt(variantIndex);
                            if (!variantTag.hasKey("machineStack", NBT.TAG_COMPOUND)) {
                                continue;
                            }
                            String typeKey = variantTag.getString("typeKey");
                            if (typeKey.isEmpty()) {
                                continue;
                            }
                            ItemStack stack = new ItemStack(variantTag.getCompoundTag("machineStack"));
                            if (stack.isEmpty()) {
                                continue;
                            }
                            String presentation = MachineStackTypeResolver.getPresentationSignature(stack);
                            if (presentation.isEmpty()) {
                                continue;
                            }
                            MachineTypeDescriptor descriptor = new MachineTypeDescriptor(typeKey,
                                  variantTag.getString("nameKey"), stack);
                            addLoaded(modId, baseKey, presentation, descriptor);
                        }
                    }
                }
                if (needsRewrite) {
                    save();
                }
            } catch (IOException | RuntimeException e) {
                entries.clear();
                Mekanism.logger.error("Failed to load wireless machine type table from {}", file, e);
            }
        }

        private synchronized MachineTypeDescriptor find(String modId, String baseKey, String presentation) {
            Map<String, List<CachedVariant>> modEntries = entries.get(normalizeModId(modId));
            if (modEntries == null) {
                return null;
            }
            List<CachedVariant> variants = modEntries.get(baseKey);
            if (variants == null) {
                return null;
            }
            for (CachedVariant variant : variants) {
                if (variant.presentation.equals(presentation)) {
                    return variant.descriptor.copy();
                }
            }
            return null;
        }

        private synchronized boolean put(String modId, String baseKey, String presentation, MachineTypeDescriptor descriptor) {
            if (baseKey.isEmpty() || presentation.isEmpty() || descriptor.getMachineStack().isEmpty()) {
                return false;
            }
            String normalizedModId = normalizeModId(modId);
            Map<String, List<CachedVariant>> modEntries = entries.computeIfAbsent(normalizedModId, ignored -> new LinkedHashMap<>());
            List<CachedVariant> variants = modEntries.computeIfAbsent(baseKey, ignored -> new ArrayList<>());
            for (CachedVariant variant : variants) {
                if (variant.presentation.equals(presentation)) {
                    return false;
                }
            }
            variants.add(new CachedVariant(presentation, descriptor.copy()));
            return true;
        }

        private void addLoaded(String modId, String baseKey, String presentation, MachineTypeDescriptor descriptor) {
            Map<String, List<CachedVariant>> modEntries = entries.computeIfAbsent(modId, ignored -> new LinkedHashMap<>());
            List<CachedVariant> variants = modEntries.computeIfAbsent(baseKey, ignored -> new ArrayList<>());
            for (CachedVariant variant : variants) {
                if (variant.presentation.equals(presentation)) {
                    return;
                }
            }
            variants.add(new CachedVariant(presentation, descriptor));
        }

        private synchronized void save() {
            File parent = file.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                Mekanism.logger.warn("Failed to create wireless machine type table directory {}", parent);
                return;
            }
            NBTTagCompound root = new NBTTagCompound();
            root.setInteger("version", DATA_VERSION);
            root.setInteger("resolverVersion", RESOLVER_VERSION);
            NBTTagList mods = new NBTTagList();
            for (Map.Entry<String, Map<String, List<CachedVariant>>> modEntry : entries.entrySet()) {
                NBTTagCompound modTag = new NBTTagCompound();
                modTag.setString("modId", modEntry.getKey());
                NBTTagList machines = new NBTTagList();
                for (Map.Entry<String, List<CachedVariant>> machineEntry : modEntry.getValue().entrySet()) {
                    NBTTagCompound machineTag = new NBTTagCompound();
                    machineTag.setString("baseKey", machineEntry.getKey());
                    NBTTagList variants = new NBTTagList();
                    for (CachedVariant variant : machineEntry.getValue()) {
                        NBTTagCompound variantTag = new NBTTagCompound();
                        variantTag.setString("typeKey", variant.descriptor.getMachineTypeKey());
                        variantTag.setString("nameKey", variant.descriptor.getMachineNameKey());
                        variantTag.setTag("machineStack", variant.descriptor.getMachineStack().writeToNBT(new NBTTagCompound()));
                        variants.appendTag(variantTag);
                    }
                    machineTag.setTag("variants", variants);
                    machines.appendTag(machineTag);
                }
                modTag.setTag("machines", machines);
                mods.appendTag(modTag);
            }
            root.setTag("mods", mods);
            try {
                CompressedStreamTools.safeWrite(root, file);
            } catch (IOException | RuntimeException e) {
                Mekanism.logger.error("Failed to save wireless machine type table to {}", file, e);
            }
        }
    }

    private static String normalizeModId(String modId) {
        return modId == null || modId.isEmpty() ? UNKNOWN_MOD_ID : modId.toLowerCase(java.util.Locale.ROOT);
    }

    private static final class CachedVariant {

        private final String presentation;
        private final MachineTypeDescriptor descriptor;

        private CachedVariant(String presentation, MachineTypeDescriptor descriptor) {
            this.presentation = presentation;
            this.descriptor = descriptor;
        }
    }
}
