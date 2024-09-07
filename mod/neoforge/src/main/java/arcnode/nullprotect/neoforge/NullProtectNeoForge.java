/*
 *    Copyright 2024 ArcNode (AFterNode)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package arcnode.nullprotect.neoforge;

import arcnode.nullprotect.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

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

        // HWID
        reg.playToServer(HardwareIdentifyResponsePacket.TYPE, HardwareIdentifyResponsePacket.CODEC, (p, c) -> {});
        reg.playToClient(HardwareIdentifyRequestPacket.TYPE, HardwareIdentifyRequestPacket.CODEC, (p, c) -> {
            c.connection().send(NullProtect.getHwidPacket());
        });

        // Mods
        reg.playToServer(ModsHashResponsePacket.TYPE, ModsHashResponsePacket.CODEC, (p, c) -> {});
        reg.playToServer(ModsHashRequestPacket.TYPE, ModsHashRequestPacket.CODEC, (p, c) -> {
            c.connection().send(NullProtect.getModsPacket());
        });
    }
}
