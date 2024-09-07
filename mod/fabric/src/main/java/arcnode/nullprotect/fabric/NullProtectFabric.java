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

import arcnode.nullprotect.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class NullProtectFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NullProtect.init();

        PayloadTypeRegistry<RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.playC2S();
        PayloadTypeRegistry<RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.playS2C();

        // HWID
        c2s.register(HardwareIdentifyResponsePacket.TYPE, HardwareIdentifyResponsePacket.CODEC);
        s2c.register(HardwareIdentifyRequestPacket.TYPE, HardwareIdentifyRequestPacket.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HardwareIdentifyRequestPacket.TYPE, (p, c) -> c.player().connection.send(NullProtect.getHwidPacket()));

        // Mods
        c2s.register(ModsHashResponsePacket.TYPE, ModsHashResponsePacket.CODEC);
        s2c.register(ModsHashRequestPacket.TYPE, ModsHashRequestPacket.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ModsHashRequestPacket.TYPE, (p, c) -> c.player().connection.send(NullProtect.getModsPacket()));
    }
}
