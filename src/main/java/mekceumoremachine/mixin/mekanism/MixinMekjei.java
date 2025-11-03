package mekceumoremachine.mixin.mekanism;

import mekanism.client.jei.RecipeRegistryHelper;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mezz.jei.api.IModRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecipeRegistryHelper.class, remap = false)
public class MixinMekjei {

    /**
     * @author sddsd2332
     * @reason 取消化学清洗机工厂在jei列表的显示
     */
    @Inject(method = "registerWasher", at = @At(value = "INVOKE", target = "Lmekanism/client/jei/RecipeRegistryHelper;registerRecipeItem(Lmezz/jei/api/IModRegistry;Lmekanism/common/block/states/BlockStateMachine$MachineType;Lmekanism/common/recipe/RecipeHandler$Recipe;)V"), cancellable = true)
    private static void removeWasherFactory(IModRegistry registry, CallbackInfo ci) {
        ci.cancel();
        registry.addRecipeCatalyst(MachineType.CHEMICAL_WASHER.getStack(), Recipe.CHEMICAL_WASHER.getJEICategory());
    }
}
