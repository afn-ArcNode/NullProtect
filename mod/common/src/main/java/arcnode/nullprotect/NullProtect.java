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

import arcnode.nullprotect.network.SingleStringData;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.Getter;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public final class NullProtect {
    public static final String MOD_ID = "nullprotect";
    private static final Logger log = LoggerFactory.getLogger("NullProtect");

    private static String hwidStr;
    @Getter
    private static String modsHash;

    public static void init() {
        // Calculate mods hash
        Thread hashCalc = new Thread(() -> {
            try {
                log.info("Calculating mods hash");
                File dir = new File(Minecraft.getInstance().gameDirectory, "mods");
                HashFunction hash = Hashing.sha256();
                StringBuilder sb = new StringBuilder();
                for (File file : dir.listFiles()) {
                    try (InputStream is = Files.newInputStream(file.toPath())) {
                        byte[] data = new byte[8];
                        is.read(data);
                        sb.append(hash.hashBytes(data));
                    }
                }

                NullProtect.modsHash = hash.hashString(sb.toString(), StandardCharsets.UTF_8).toString();
                log.info("Mods hash calculation completed: {}", NullProtect.modsHash);
            } catch (Throwable t) {
                Minecraft.getInstance().delayCrash(new CrashReport("NullProtect/HashCalc", t));
            }
        });
        hashCalc.setName("NullProtect/HashCalc");
        hashCalc.start();
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
                        new SingleStringData(
                                getHwidString()
                        )
                )
        );
    }

    public static ServerboundCustomPayloadPacket getModsPacket() {
        return new ServerboundCustomPayloadPacket(
                new ModsHashResponsePacket(
                        new SingleStringData(
                                getModsHash()
                        )
                )
        );
    }
}
