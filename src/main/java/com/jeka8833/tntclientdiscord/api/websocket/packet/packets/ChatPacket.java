package com.jeka8833.tntclientdiscord.api.websocket.packet.packets;

import com.jeka8833.tntclientdiscord.TNTChatManager;
import com.jeka8833.tntclientdiscord.api.websocket.packet.Packet;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketInputStream;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketOutputStream;
import okhttp3.WebSocket;

import java.io.IOException;
import java.util.UUID;

public class ChatPacket implements Packet {
    private UUID user;
    private String text;

    @SuppressWarnings("unused")
    public ChatPacket() {
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {

    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        text = stream.readUTF();
    }

    @Override
    public void clientProcess(WebSocket socket) {
        TNTChatManager.sendMessage(user, text);
    }
}
