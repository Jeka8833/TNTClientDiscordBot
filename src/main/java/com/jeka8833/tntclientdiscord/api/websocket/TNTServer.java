package com.jeka8833.tntclientdiscord.api.websocket;

import com.jeka8833.tntclientdiscord.api.websocket.packet.Packet;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
public class TNTServer {

    private static final long RECONNECT_DELAY = 30_000;

    private static final Logger logger = LoggerFactory.getLogger(TNTServer.class);

    private WebSocket webSocket;
    private long nextReconnect = 0;

    @Value("${tntserver.ip}")
    private String serverIP;

    @Value("${tntserver.user}")
    private UUID user;

    @Value("${tntserver.password}")
    private UUID password;


    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void checkConnection() {
        try {
            if (webSocket == null || (!TNTServerSocket.isConnect() && System.currentTimeMillis() > nextReconnect)) {
                nextReconnect = System.currentTimeMillis() + RECONNECT_DELAY;

                if (webSocket != null) webSocket.close(1000, null);
                webSocket = TNTServerSocket.connect(serverIP, user, password);
            }
        } catch (Exception e) {
            logger.warn("Error while checking connection: ", e);
        }
    }

    public void send(Packet packet) {
        TNTServerSocket.send(webSocket, packet);
    }
}
