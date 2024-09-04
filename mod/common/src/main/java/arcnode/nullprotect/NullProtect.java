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
import com.google.common.hash.Hashing;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

import java.nio.charset.StandardCharsets;

public final class NullProtect {
    public static final String MOD_ID = "nullprotect";

    private static String hwidStr;

    public static void init() {
        // Write common init code here.
    }

    public static String getHwidString() {
        if (hwidStr == null) {
            try {
                SystemInfo si = new SystemInfo();
                HardwareAbstractionLayer hardware = si.getHardware();
                ComputerSystem system = hardware.getComputerSystem();
                CentralProcessor processor = hardware.getProcessor();

                StringBuilder sb = new StringBuilder();
                sb.append(system.getHardwareUUID());
                sb.append(system.getModel());
                for (CentralProcessor.PhysicalProcessor pp : processor.getPhysicalProcessors()) {
                    sb.append(pp.getIdString());
                }

                hwidStr = Hashing.sha256().hashString(sb.toString(), StandardCharsets.UTF_8)
                        .toString()
                        .substring(0, 32);
            } catch (Throwable t) {
                throw new RuntimeException("Unable to calculate HWID", t);
            }
        }
        return hwidStr;
    }

    public static ServerboundCustomPayloadPacket getHwidPacket() {
        return new ServerboundCustomPayloadPacket(
                new HardwareIdentifyResponsePacket(
                        new HardwareIdentifyData(
                                getHwidString()
                        )
                )
        );
    }
}
