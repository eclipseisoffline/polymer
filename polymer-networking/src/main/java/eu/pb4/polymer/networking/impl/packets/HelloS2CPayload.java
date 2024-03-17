package eu.pb4.polymer.networking.impl.packets;

import eu.pb4.polymer.networking.api.PolymerNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

public record HelloS2CPayload() implements CustomPayload {
    public static final Id<HelloS2CPayload> ID = PolymerNetworking.id("polymer", "hello");

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
