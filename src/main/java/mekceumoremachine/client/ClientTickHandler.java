package mekceumoremachine.client;

import mekanism.common.base.IFactory.RecipeType;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.item.ItemBlockMachine;
import mekanism.common.util.LangUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public void RemoveTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ItemBlockMachine machine) {
            MachineType type = MachineType.get(stack);
            if (type.isFactory()) {
                RecipeType recipeType = machine.getRecipeTypeOrNull(stack);
                if (recipeType == RecipeType.WASHER || recipeType == RecipeType.Dissolution || recipeType == RecipeType.OXIDIZER) {
                    if (!event.getToolTip().isEmpty()) {
                        List<String> info = new ArrayList<>(event.getToolTip());
                        event.getToolTip().clear();
                        event.getToolTip().add(info.get(0));
                        event.getToolTip().add(LangUtils.localize("tooltip.mekceumoremachine.remove"));
                    }
                }
            }
        }
    }


}
