package arcnode.nullprotect;

import arcnode.nullprotect.network.HardwareIdentifyData;
import arcnode.nullprotect.network.PacketIO;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record HardwareIdentifyPacket(HardwareIdentifyData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HardwareIdentifyPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PacketIO.NAMESPACE, PacketIO.PATH_HWID));
    public static final StreamCodec<FriendlyByteBuf, HardwareIdentifyPacket> CODEC = CustomPacketPayload.codec(HardwareIdentifyPacket::encode, HardwareIdentifyPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static HardwareIdentifyPacket decode(FriendlyByteBuf buf) {
        return new HardwareIdentifyPacket(new HardwareIdentifyData("dum"));
    }

    private void encode(FriendlyByteBuf buf) {
        PacketIO.encode(buf, this.data);
    }
}
