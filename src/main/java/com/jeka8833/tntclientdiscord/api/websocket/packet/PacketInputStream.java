package com.jeka8833.tntclientdiscord.api.websocket.packet;

import com.jeka8833.tntclientdiscord.api.websocket.TNTServerSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class PacketInputStream extends DataInputStream {
    private static final Logger logger = LogManager.getLogger(PacketInputStream.class);

    public final Packet packet;

    public PacketInputStream(final byte[] buffer) throws Exception {
        super(new ByteArrayInputStream(buffer));
        packet = TNTServerSocket.registeredPackets.get(readByte()).getDeclaredConstructor().newInstance();

        logger.debug("Packet received: {}", packet);
    }

    @Contract(" -> new")
    public final UUID readUUID() throws IOException {
        return new UUID(readLong(), readLong());
    }
}
