package eu.pb4.polymer.mixin.other;

import com.mojang.serialization.Lifecycle;
import eu.pb4.polymer.api.utils.PolymerObject;
import eu.pb4.polymer.api.utils.PolymerUtils;
import eu.pb4.polymer.impl.PolymerImplUtils;
import eu.pb4.polymer.impl.interfaces.RegistryExtension;
import eu.pb4.polymer.impl.other.DeferredRegistryEntry;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(SimpleRegistry.class)
public abstract class SimpleRegistryMixin<T> implements RegistryExtension<T>, Registry<T> {
    @Shadow private volatile Map<TagKey<T>, RegistryEntryList.Named<T>> tagToEntryList;

    @Shadow public abstract RegistryEntry.Reference<T> add(RegistryKey<T> key, T entry, Lifecycle lifecycle);

    @Nullable
    @Unique
    private List<T> polymer_objects = null;

    @Unique
    private List<DeferredRegistryEntry<T>> polymer_deferredRegistration = new ArrayList<>();

    @Unique
    private boolean polymer_deferRegistration = true;

    /*@Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private <V extends T> void polymer_deferRegistration(RegistryKey<T> key, T entry, Lifecycle lifecycle, CallbackInfoReturnable<RegistryEntry<T>> cir) {
        if (entry instanceof PolymerBlock && this.polymer_deferRegistration) {
            this.polymer_deferredRegistration.add(new DeferredRegistryEntry<>(key, entry, lifecycle));
            cir.setReturnValue((RegistryEntry<T>) ((Block) entry).getRegistryEntry());
        }
    }*/

    @Inject(method = "freeze", at = @At("HEAD"))
    private void polymer_registerDeferred(CallbackInfoReturnable<Registry<T>> cir) {
        this.polymer_deferRegistration = false;
        for (var obj : this.polymer_deferredRegistration) {
            this.add(obj.registryKey(), obj.entry(), obj.lifecycle());
        }
        this.polymer_deferredRegistration.clear();
    }

    @Inject(method = "set", at = @At("TAIL"))
    private <V extends T> void polymer_storeStatus(int rawId, RegistryKey<T> key, T value, Lifecycle lifecycle, CallbackInfoReturnable<RegistryEntry<T>> cir) {
        this.polymer_objects = null;
        if (PolymerObject.is(value)) {
            RegistrySyncUtils.setServerEntry(this, value);
        }

        PolymerImplUtils.invokeRegistered((Registry<Object>) this, value);
    }

    @Override
    public List<T> polymer_getEntries() {
        if (this.polymer_objects == null) {
            this.polymer_objects = new ArrayList<>();
            for (var obj : this) {
                if (PolymerUtils.isServerOnly(obj)) {
                    this.polymer_objects.add(obj);
                }
            }
        }

        return this.polymer_objects;
    }

    @Override
    public Map<TagKey<T>, RegistryEntryList.Named<T>> polymer_getTagsInternal() {
        return this.tagToEntryList;
    }
}
