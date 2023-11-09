package com.jeka8833.tntclientdiscord.api.websocket.packet.packets;

import com.jeka8833.tntclientdiscord.api.websocket.TNTServerSocket;
import com.jeka8833.tntclientdiscord.api.websocket.packet.Packet;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketInputStream;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketOutputStream;
import okhttp3.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class AuthPacket implements Packet {
    private final @Nullable UUID user;
    private final @Nullable UUID password;

    @SuppressWarnings("unused")
    public AuthPacket() {
        this(null, null);
    }

    public AuthPacket(@Nullable UUID user, @Nullable UUID password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (user == null || password == null) throw new NullPointerException("User or password is null.");

        stream.writeUUID(user);
        stream.writeUUID(password);
    }

    @Override
    public void read(PacketInputStream stream) {
    }

    @Override
    public void clientProcess(WebSocket socket) {
        TNTServerSocket.setConnectedState();
    }
}
