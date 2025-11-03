package mekceumoremachine.common.tile.generator;

import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.component.TileComponentUpgrade;

public class TileEntityBigWindGenerator extends TileEntityBaseWindGenerator {

    public TileComponentUpgrade upgradeComponent;

    public TileEntityBigWindGenerator() {
        super("BigWindGenerator");
    }

    @Override
    public int processes() {
        return 1024 * MekanismConfig.current().mekce.MAXThreadUpgrade.val();
    }



}
