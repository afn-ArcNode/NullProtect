package arcnode.nullprotect.fabric;

import arcnode.nullprotect.HardwareIdentifyPacket;
import arcnode.nullprotect.NullProtect;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class NullProtectFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        PayloadTypeRegistry.playC2S().register(HardwareIdentifyPacket.TYPE, HardwareIdentifyPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(HardwareIdentifyPacket.TYPE, HardwareIdentifyPacket.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HardwareIdentifyPacket.TYPE, (p, c) -> {
            c.player().connection.send(NullProtect.getHwidPacket());
        });
    }
}
