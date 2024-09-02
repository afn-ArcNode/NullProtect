package arcnode.nullprotect.neoforge;

import arcnode.nullprotect.HardwareIdentifyPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import arcnode.nullprotect.NullProtect;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(NullProtect.MOD_ID)
public final class NullProtectNeoForge {
    public NullProtectNeoForge(IEventBus eb) {
        // Run our common setup.
        NullProtect.init();

        eb.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1").optional();
        reg.playToServer(HardwareIdentifyPacket.TYPE, HardwareIdentifyPacket.CODEC, (p, c) -> {});
        reg.playToClient(HardwareIdentifyPacket.TYPE, HardwareIdentifyPacket.CODEC, (p, c) -> {
            c.connection().send(NullProtect.getHwidPacket());
        });
    }
}
