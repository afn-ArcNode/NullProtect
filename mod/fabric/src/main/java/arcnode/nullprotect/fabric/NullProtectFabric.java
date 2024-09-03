package arcnode.nullprotect.fabric;

import arcnode.nullprotect.HardwareIdentifyRequestPacket;
import arcnode.nullprotect.HardwareIdentifyResponsePacket;
import arcnode.nullprotect.NullProtect;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class NullProtectFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        PayloadTypeRegistry.playC2S().register(HardwareIdentifyResponsePacket.TYPE, HardwareIdentifyResponsePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(HardwareIdentifyRequestPacket.TYPE, HardwareIdentifyRequestPacket.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HardwareIdentifyRequestPacket.TYPE, (p, c) -> {
            c.player().connection.send(NullProtect.getHwidPacket());
        });
    }
}
