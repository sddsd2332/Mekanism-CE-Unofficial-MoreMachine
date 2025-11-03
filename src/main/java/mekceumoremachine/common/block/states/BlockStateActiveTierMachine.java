package mekceumoremachine.common.block.states;

import mekceumoremachine.common.block.BlockTierMachine;
import net.minecraft.block.properties.PropertyBool;

public class BlockStateActiveTierMachine extends BlockStateTierMachine {

    public static final PropertyBool activeProperty = PropertyBool.create("active");

    public BlockStateActiveTierMachine(BlockTierMachine block) {
        super(block, activeProperty);
    }
}
