package arcnode.nullprotect;

import arcnode.nullprotect.network.HardwareIdentifyData;
import arcnode.nullprotect.network.PacketIO;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record HardwareIdentifyResponsePacket(HardwareIdentifyData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HardwareIdentifyResponsePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PacketIO.NAMESPACE, PacketIO.PATH_HWID_RESPONSE));
    public static final StreamCodec<FriendlyByteBuf, HardwareIdentifyResponsePacket> CODEC = CustomPacketPayload.codec(HardwareIdentifyResponsePacket::encode, HardwareIdentifyResponsePacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static HardwareIdentifyResponsePacket decode(FriendlyByteBuf buf) {
        return new HardwareIdentifyResponsePacket(PacketIO.decode(buf));
    }

    private void encode(FriendlyByteBuf buf) {
        PacketIO.encode(buf, this.data);
    }
}
