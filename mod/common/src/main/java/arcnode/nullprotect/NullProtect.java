package arcnode.nullprotect;

import arcnode.nullprotect.network.HardwareIdentifyData;
import com.google.common.hash.Hashing;
import net.minecraft.client.multiplayer.ClientPacketListener;
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

                hwidStr = Hashing.sha384().hashString(sb.toString(), StandardCharsets.UTF_8)
                        .toString();
            } catch (Throwable t) {
                throw new RuntimeException("Unable to calculate HWID", t);
            }
        }
        return hwidStr;
    }

    public static ServerboundCustomPayloadPacket getHwidPacket() {
        return new ServerboundCustomPayloadPacket(
                new HardwareIdentifyPacket(
                        new HardwareIdentifyData(
                                getHwidString()
                        )
                )
        );
    }
}
