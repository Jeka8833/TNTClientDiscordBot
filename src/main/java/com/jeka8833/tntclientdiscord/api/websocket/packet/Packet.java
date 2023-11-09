package com.jeka8833.tntclientdiscord.api.websocket.packet;

import okhttp3.WebSocket;

import java.io.IOException;

public interface Packet {
    void write(final PacketOutputStream stream) throws IOException;

    void read(final PacketInputStream stream) throws IOException;

    void clientProcess(final WebSocket socket);
}
