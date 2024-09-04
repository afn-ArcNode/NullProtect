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
