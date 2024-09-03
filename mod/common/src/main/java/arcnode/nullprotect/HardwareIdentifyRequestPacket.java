package arcnode.nullprotect;

import arcnode.nullprotect.network.HardwareIdentifyData;
import arcnode.nullprotect.network.PacketIO;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record HardwareIdentifyRequestPacket(HardwareIdentifyData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HardwareIdentifyRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PacketIO.NAMESPACE, PacketIO.PATH_HWID_REQUEST));
    public static final StreamCodec<FriendlyByteBuf, HardwareIdentifyRequestPacket> CODEC = CustomPacketPayload.codec(HardwareIdentifyRequestPacket::encode, HardwareIdentifyRequestPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static HardwareIdentifyRequestPacket decode(FriendlyByteBuf buf) {
        return new HardwareIdentifyRequestPacket(PacketIO.decode(buf));
    }

    private void encode(FriendlyByteBuf buf) {
        PacketIO.encode(buf, this.data);
    }
}
