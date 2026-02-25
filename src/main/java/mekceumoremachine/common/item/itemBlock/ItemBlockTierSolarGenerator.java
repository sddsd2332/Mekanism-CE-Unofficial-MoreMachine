package mekceumoremachine.common.item.itemBlock;

import mekanism.common.config.MekanismConfig;
import mekceumoremachine.common.tier.MachineTier;
import mekceumoremachine.common.tile.generator.TileEntityTierSolarGenerator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ItemBlockTierSolarGenerator extends ItemBlockMekceuMoreMachineTier {

    public ItemBlockTierSolarGenerator(Block block) {
        super(block, "TierSolarGenerator");
    }

    @Override
    public void setTierMachine(TileEntity tileEntity, ItemStack stack) {
        if (tileEntity instanceof TileEntityTierSolarGenerator tile) {
            tile.tier = MachineTier.values()[getBaseTier(stack).ordinal()];
        }
    }

    @Override
    public double getMachineStorage() {
        return MekanismConfig.current().generators.solarGeneratorStorage.val();
    }
}
