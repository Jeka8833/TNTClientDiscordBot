package com.jeka8833.tntclientdiscord.api.websocket.packet.packets;

import com.jeka8833.tntclientdiscord.StaticContextAccessor;
import com.jeka8833.tntclientdiscord.api.websocket.TNTServer;
import com.jeka8833.tntclientdiscord.api.websocket.packet.Packet;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketInputStream;
import com.jeka8833.tntclientdiscord.api.websocket.packet.PacketOutputStream;
import com.jeka8833.tntclientdiscord.database.Muted;
import com.jeka8833.tntclientdiscord.database.MutedRepository;
import okhttp3.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class MutePacket implements Packet {

    private Collection<PlayerMute> players;

    @SuppressWarnings("unused")
    public MutePacket() {
    }

    public MutePacket(Collection<Muted> mutedCollection) {
        players = new ArrayList<>();
        for (Muted muted : mutedCollection) {
            if (muted.isMuted()) {
                players.add(new PlayerMute(muted.getPlayer(), muted.getReason(), muted.getUnmuteTime().toString()));
            }
        }
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeInt(players.size());
        for (PlayerMute player : players) {
            stream.writeUUID(player.user);
            stream.writeUTF(player.description);
            stream.writeUTF(player.unmuteTime);
        }
    }

    @Override
    public void read(PacketInputStream stream) {
    }

    @Override
    public void clientProcess(WebSocket socket) {
        sentLastedList();
    }

    public static void sentLastedList() {
        MutedRepository mutedRepository = StaticContextAccessor.getBean(MutedRepository.class);
        TNTServer tntServer = StaticContextAccessor.getBean(TNTServer.class);

        tntServer.send(new MutePacket(mutedRepository.findAll()));
    }

    private record PlayerMute(UUID user, String description, String unmuteTime) {
    }
}
