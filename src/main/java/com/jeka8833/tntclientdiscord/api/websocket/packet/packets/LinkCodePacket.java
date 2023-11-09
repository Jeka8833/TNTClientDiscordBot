package com.jeka8833.tntclientdiscord.api.websocket.packet.packets;

import com.jeka8833.tntclientdiscord.MessageUtil;
import com.jeka8833.tntclientdiscord.StaticContextAccessor;
import com.jeka8833.tntclientdiscord.api.websocket.TNTServerSocket;
import com.jeka8833.tntclientdiscord.api.websocket.packet.Packet;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketInputStream;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketOutputStream;
import com.jeka8833.tntclientdiscord.security.TokenManager;
import okhttp3.WebSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class LinkCodePacket implements Packet {
    public static final int INTERNAL_ERROR = 0;
    public static final int CONNECTION_ERROR = 1;
    public static final int MESSAGE_GOOD_LINK = 2;
    public static final int MESSAGE_BAD_LINK = 3;
    public static final int MESSAGE_GOOD_UNLINK = 4;
    public static final int MESSAGE_BAD_UNLINK = 5;
    public static final int TRY_LINK = 6;
    public static final int TRY_UNLINK = 7;

    private static final Logger logger = LogManager.getLogger(TNTServerSocket.class);

    private UUID user;
    private int code;
    private int statusCode;

    @SuppressWarnings("unused")
    public LinkCodePacket() {
        this(null, Integer.MIN_VALUE, INTERNAL_ERROR);
    }

    public LinkCodePacket(UUID user, int code, int statusCode) {
        this.user = user;
        this.code = code;
        this.statusCode = statusCode;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeInt(code);
        stream.writeInt(statusCode);
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        user = stream.readUUID();
        code = stream.readInt();
        statusCode = stream.readInt();
    }

    @Override
    public void clientProcess(WebSocket socket) {
        try {
            if (statusCode == TRY_UNLINK) {
                Optional<Long> discordID = StaticContextAccessor.getBean(TokenManager.class)
                        .removeLink(user);

                if (discordID.isPresent()) {
                    TNTServerSocket.send(socket, new LinkCodePacket(user, code, MESSAGE_GOOD_UNLINK));

                    MessageUtil.sendPrivateMessage(discordID.get(), "You have been unlinked from TNTClient.");
                } else {
                    TNTServerSocket.send(socket, new LinkCodePacket(user, code, MESSAGE_BAD_UNLINK));
                }
            } else if (statusCode == TRY_LINK) {
                Optional<Long> discordID = StaticContextAccessor.getBean(TokenManager.class)
                        .validateToken(user, code);

                if (discordID.isPresent()) {
                    TNTServerSocket.send(socket, new LinkCodePacket(user, code, MESSAGE_GOOD_LINK));

                    MessageUtil.sendPrivateMessage(discordID.get(), "You have been linked to TNTClient. " +
                            "If it was not you, write the command \"@discordlink remove\" in your TNTClient.");
                } else {
                    TNTServerSocket.send(socket, new LinkCodePacket(user, code, MESSAGE_BAD_LINK));
                }
            } else {
                TNTServerSocket.send(socket, new LinkCodePacket(user, code, INTERNAL_ERROR));
            }
        } catch (Exception e) {
            TNTServerSocket.send(socket, new LinkCodePacket(user, code, INTERNAL_ERROR));

            logger.warn("Fail process LinkCodePacket", e);
        }
    }
}
