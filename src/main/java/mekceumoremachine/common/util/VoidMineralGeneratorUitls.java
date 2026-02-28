package mekceumoremachine.common.util;

import mekceumoremachine.common.config.MoreMachineConfig;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class VoidMineralGeneratorUitls {

    private static final List<ItemStack> canOre = new ArrayList<>();

    private VoidMineralGeneratorUitls() {
    }

    /**
     * Populate the internal canOre list from registered OreDictionary names.
     * Call this after mods have registered their OreDictionary entries (e.g., postInit).
     */
    public static synchronized void populateCanOre() {
        canOre.clear();
        String[] oreNames = OreDictionary.getOreNames();
        String[] blacklist = MoreMachineConfig.current().config.OreGenerationBlacklist.get();
        boolean reversal = MoreMachineConfig.current().config.OreGenerationBlacklistReversal.val();

        if (oreNames == null || oreNames.length == 0) return;

        // Collect candidate stacks for ore* names
        List<ItemStack> candidates = Arrays.stream(oreNames)
                .filter(Objects::nonNull)
                .filter(n -> n.startsWith("ore"))
                .filter(n -> {
                    if (!reversal) return true;
                    if (blacklist == null || blacklist.length == 0) return false;
                    return Arrays.stream(blacklist).filter(Objects::nonNull).anyMatch(b -> b.equals(n));
                })
                .map(n -> {
                    List<ItemStack> stacks = OreDictionary.getOres(n);
                    return (stacks != null && !stacks.isEmpty()) ? stacks.get(0).copy() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!reversal && blacklist != null && blacklist.length > 0) {
            List<String> blackListNames = Arrays.stream(blacklist).filter(Objects::nonNull).collect(Collectors.toList());
            candidates.removeIf(candidate ->
                    blackListNames.stream().anyMatch(b -> {
                        List<ItemStack> blackStacks = OreDictionary.getOres(b);
                        return blackStacks != null && blackStacks.stream().anyMatch(black -> OreDictionary.itemMatches(black, candidate, false));
                    })
            );
        }

        // ensure no empty stacks and normalize to count 1
        candidates.stream().filter(s -> !s.isEmpty()).map(s -> {
            ItemStack c = s.copy();
            c.setCount(1);
            return c;
        }).forEach(canOre::add);
    }

    @Nonnull
    public static synchronized List<ItemStack> getCanOre() {
        return new ArrayList<>(canOre);
    }

}
