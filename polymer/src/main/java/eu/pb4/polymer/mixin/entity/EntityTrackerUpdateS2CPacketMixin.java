package eu.pb4.polymer.mixin.entity;

import eu.pb4.polymer.api.block.PolymerBlock;
import eu.pb4.polymer.api.block.PolymerBlockUtils;
import eu.pb4.polymer.api.entity.PolymerEntity;
import eu.pb4.polymer.api.item.PolymerItem;
import eu.pb4.polymer.api.item.PolymerItemUtils;
import eu.pb4.polymer.api.utils.PolymerUtils;
import eu.pb4.polymer.impl.client.ClientUtils;
import eu.pb4.polymer.impl.entity.InternalEntityHelpers;
import eu.pb4.polymer.impl.interfaces.EntityAttachedPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public class EntityTrackerUpdateS2CPacketMixin {
    @Shadow @Final private int id;

    @Shadow @Final private List<DataTracker.SerializedEntry<?>> trackedValues;

    @Nullable
    private List<DataTracker.SerializedEntry<?>> polymer_parseEntries() {
        Entity entity = EntityAttachedPacket.get(this);
        if (entity == null || entity.getId() != this.id) {
            return this.trackedValues != null ? new ArrayList<>(this.trackedValues) : null;
        }

        var entries = new ArrayList<DataTracker.SerializedEntry<?>>();
        var player = PolymerUtils.getPlayer();

        if (entity instanceof PolymerEntity polymerEntity && InternalEntityHelpers.canPatchTrackedData(player, entity)) {
            var legalTrackedData = InternalEntityHelpers.getExampleTrackedDataOfEntityType((polymerEntity.getPolymerEntityType(player)));

            if (legalTrackedData != null && legalTrackedData.size() > 0) {
                if (this.trackedValues != null) {
                    for (var entry : this.trackedValues) {
                        var x = legalTrackedData.get(entry.id());
                        if (x != null && x.getData().getType() == entry.handler()) {
                            entries.add(entry);
                            break;
                        }
                    }
                }
            }
            polymerEntity.modifyRawTrackedData(entries, player);

        } else if (this.trackedValues == null) {
            return null;
        } else {
            entries.addAll(this.trackedValues);
        }

        final var isItemFrame = entity instanceof ItemFrameEntity;
        final var isMinecart = entity instanceof AbstractMinecartEntity;
        final var size = entries.size();
        for (int i = 0; i < size; i++) {
            var entry = entries.get(i);

            if (isItemFrame && entry.id() == ItemFrameEntityAccessor.getITEM_STACK().getId() && entry.value() instanceof ItemStack stack) {
                var polymerStack = PolymerItemUtils.getPolymerItemStack(stack, PolymerUtils.getPlayer());

                if (!stack.hasCustomName() && !(stack.getItem() instanceof PolymerItem polymerItem && polymerItem.showDefaultNameInItemFrames())) {
                    var nbtCompound = polymerStack.getSubNbt("display");
                    if (nbtCompound != null) {
                        var name = nbtCompound.get("Name");
                        if (name != null) {
                            polymerStack.getNbt().put(PolymerItemUtils.ITEM_FRAME_NAME_TAG, name);
                        }
                        nbtCompound.remove("Name");
                    }
                }

                entries.set(i, new DataTracker.SerializedEntry(entry.id(), entry.handler(), polymerStack));
            } else if (isMinecart && entry.id() == AbstractMinecartEntityAccessor.getCUSTOM_BLOCK_ID().getId()) {
                entries.set(i, new DataTracker.SerializedEntry(entry.id(), entry.handler(), Block.getRawIdFromState(PolymerBlockUtils.getPolymerBlockState(Block.getStateFromRawId((int) entry.value()), player))));
            }
        }

        return entries;
    }

    @ModifyArg(method = "write(Lnet/minecraft/network/PacketByteBuf;)V", at = @At(value = "INVOKE", target = "net/minecraft/network/packet/s2c/play/EntityTrackerUpdateS2CPacket.write(Ljava/util/List;Lnet/minecraft/network/PacketByteBuf;)V", ordinal = 0))
    private List<DataTracker.SerializedEntry<?>> polymer_replaceWithPolymer(List<DataTracker.Entry<?>> value) {
        return this.polymer_parseEntries();
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "trackedValues", at = @At("RETURN"), cancellable = true)
    private void polymer_replaceItemsWithPolymerOnes(CallbackInfoReturnable<List<DataTracker.SerializedEntry<?>>> cir) {
        if (ClientUtils.isSingleplayer() && this.trackedValues != null) {
            var list = this.polymer_parseEntries();

            ServerPlayerEntity player = ClientUtils.getPlayer();

            for (int i = 0; i < list.size(); i++) {
                var entry = list.get(i);
                if (entry.value() instanceof Optional<?> optionalO && optionalO.isPresent()
                        && optionalO.get() instanceof BlockState state && state.getBlock() instanceof PolymerBlock polymerBlock) {
                    list.set(i, new DataTracker.SerializedEntry(entry.id(), entry.handler(), Optional.of(PolymerBlockUtils.getBlockStateSafely(polymerBlock, state, player))));
                }
            }

            cir.setReturnValue(list);
        }
    }
}
