package com.jeka8833.tntclientdiscord.api.websocket.packet;

import com.jeka8833.tntclientdiscord.api.websocket.TNTServerSocket;
import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PacketOutputStream extends DataOutputStream {
    private static final Logger logger = LogManager.getLogger(PacketOutputStream.class);

    public PacketOutputStream(@NotNull Class<? extends Packet> type) throws IOException {
        super(new ByteArrayOutputStream());

        out.write(TNTServerSocket.registeredPackets.getKey(type));

        logger.debug("Packet send: {}", type);
    }

    public void writeUUID(@NotNull UUID uuid) throws IOException {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    public ByteString getByteString() {
        return new ByteString(((ByteArrayOutputStream) this.out).toByteArray());
    }
}
