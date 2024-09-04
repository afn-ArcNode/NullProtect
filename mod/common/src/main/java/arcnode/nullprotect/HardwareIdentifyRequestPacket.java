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
