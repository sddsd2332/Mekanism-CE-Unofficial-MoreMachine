package mekceumoremachine.mixin.ae2;

import appeng.api.networking.energy.IAEPowerStorage;
import appeng.core.AELog;
import appeng.me.cache.EnergyGridCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.util.Set;

@Mixin(value = EnergyGridCache.class, remap = false)
public class MixinEnergyGridCache {

    @Shadow
    @Final
    private Set<IAEPowerStorage> requesters;

    @Shadow
    @Final
    private Set<IAEPowerStorage> providers;

    @Shadow
    @Final
    private Set<IAEPowerStorage> requesterToAdd;

    @Shadow
    @Final
    private Set<IAEPowerStorage> providersToAdd;

    @Redirect(method = "addRequester", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean safeAddRequester(Set<?> set, Object element) {
        try {
            @SuppressWarnings("unchecked")
            boolean added = ((Set) set).add(element);
            return added;
        } catch (Throwable t) {
            try {
                AELog.info("[Mixin] MixinEnergyGridCache.addRequester: exception when adding to set (%s). Deferring add.", set.getClass().getName());

                // Enhanced diagnostics: try to extract fastutil internals if present
                try {
                    Class<?> cls = set.getClass();
                    // try common field names used by fastutil ReferenceLinkedOpenHashSet implementation
                    Field fKey = null;
                    Field fN = null;
                    Field fMask = null;
                    try {
                        fKey = cls.getDeclaredField("key");
                        fKey.setAccessible(true);
                    } catch (NoSuchFieldException ignored) {
                    }
                    try {
                        fN = cls.getDeclaredField("n");
                        fN.setAccessible(true);
                    } catch (NoSuchFieldException ignored) {
                    }
                    try {
                        fMask = cls.getDeclaredField("mask");
                        fMask.setAccessible(true);
                    } catch (NoSuchFieldException ignored) {
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("[MixinDiag] Set class: ").append(cls.getName()).append("; ");
                    if (fKey != null) {
                        Object keyObj = fKey.get(set);
                        if (keyObj != null && keyObj.getClass().isArray()) {
                            sb.append("key.length=").append(java.lang.reflect.Array.getLength(keyObj)).append("; ");
                        } else {
                            sb.append("key=null; ");
                        }
                    }
                    if (fN != null) {
                        Object val = fN.get(set);
                        sb.append("n=").append(val).append("; ");
                    }
                    if (fMask != null) {
                        Object val = fMask.get(set);
                        sb.append("mask=").append(val).append("; ");
                    }

                    AELog.info(sb.toString());
                } catch (Throwable refl) {
                    AELog.info("[MixinDiag] failed to extract fastutil internals: %s", refl.getMessage());
                }

            } catch (Throwable ignore) {
            }

            if (element instanceof IAEPowerStorage) {
                IAEPowerStorage storage = (IAEPowerStorage) element;
                try {
                    if (set == this.requesters) {
                        this.requesterToAdd.add(storage);
                        return true;
                    }
                    if (set == this.providers) {
                        this.providersToAdd.add(storage);
                        return true;
                    }
                } catch (Throwable ignore) {
                }
            }

            return false;
        }
    }

    @Redirect(method = "addProvider", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean safeAddProvider(Set<?> set, Object element) {
        return safeAddRequester(set, element);
    }
}

