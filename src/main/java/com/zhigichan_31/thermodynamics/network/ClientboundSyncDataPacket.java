package com.zhigichan_31.thermodynamics.network;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record ClientboundSyncDataPacket(float stamina, float weight, boolean isExhausted) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thermodynamics.MODID, "sync_data");
    public static final Type<ClientboundSyncDataPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ClientboundSyncDataPacket> CODEC = StreamCodec.of(
            (buf, val) -> { buf.writeFloat(val.stamina); buf.writeFloat(val.weight); buf.writeBoolean(val.isExhausted); },
            buf -> new ClientboundSyncDataPacket(buf.readFloat(), buf.readFloat(), buf.readBoolean())
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
