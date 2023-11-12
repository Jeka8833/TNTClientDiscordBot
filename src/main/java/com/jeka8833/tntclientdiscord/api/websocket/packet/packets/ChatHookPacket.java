package com.jeka8833.tntclientdiscord.api.websocket.packet.packets;

import com.jeka8833.tntclientdiscord.ServerType;
import com.jeka8833.tntclientdiscord.StaticContextAccessor;
import com.jeka8833.tntclientdiscord.TNTChatManager;
import com.jeka8833.tntclientdiscord.api.websocket.TNTServer;
import com.jeka8833.tntclientdiscord.api.websocket.packet.Packet;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketInputStream;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketOutputStream;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class ChatHookPacket implements Packet {
    private static final UUID EMPTY_UUID = new UUID(0, 0);

    private UUID sender;
    private UUID receiver;
    private String server;
    private String text;

    @SuppressWarnings("unused")
    public ChatHookPacket() {
    }

    public ChatHookPacket(@NotNull UUID sender, @Nullable UUID receiver,
                          @Nullable ServerType server, @NotNull String text) {
        this.sender = sender;
        this.receiver = receiver == null ? EMPTY_UUID : receiver;
        this.server = server == null ? "" : server.getActualServerBrand();
        this.text = text;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(sender);
        stream.writeUUID(receiver);
        stream.writeUTF(server);
        stream.writeUTF(text);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        sender = stream.readUUID();
        receiver = stream.readUUID();
        server = stream.readUTF();
        text = stream.readUTF();
    }

    @Nullable
    public UUID getReceiver() {
        if (receiver.equals(EMPTY_UUID)) return null;
        return receiver;
    }

    @NotNull
    public ServerType getServer() {
        return ServerType.getServer(server);
    }

    @Override
    public void clientProcess(WebSocket socket) {
        TNTChatManager.sendMessage(sender, getReceiver(), getServer(), text);
    }

    public static void sendMessage(@NotNull UUID sender, @Nullable UUID receiver,
                                   @Nullable ServerType server, @NotNull String text) {
        TNTServer tntServer = StaticContextAccessor.getBean(TNTServer.class);

        tntServer.send(new ChatHookPacket(sender, receiver, server, text));
    }
}
