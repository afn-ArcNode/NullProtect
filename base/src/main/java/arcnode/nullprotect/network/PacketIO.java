package arcnode.nullprotect.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class PacketIO {
    public static final String NAMESPACE = "nullprotect";
    public static final String PATH_HWID = "hwid";

    public static void encode(ByteBuf buf, HardwareIdentifyData data) {
        byte[] bytes = data.value().getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static byte[] dummy() {
        byte[] dum = new byte[16];
        ByteBuf bb = Unpooled.wrappedBuffer(dum);
        bb.writerIndex(0);
        encode(bb, new HardwareIdentifyData(""));
        return dum;
    }

    public static HardwareIdentifyData decode(ByteBuf buf) {
        int length = buf.readInt();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        buf.readBytes(buf.readableBytes());
        return new HardwareIdentifyData(new String(bytes, StandardCharsets.UTF_8));
    }

    public static HardwareIdentifyData decode(byte[] data) {
        ByteBuf bb = Unpooled.copiedBuffer(data);
        bb.readerIndex(0);
        return decode(bb);
    }
}
