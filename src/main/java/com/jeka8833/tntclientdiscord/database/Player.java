package com.jeka8833.tntclientdiscord.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity(name = "tnd_players")
public class Player {
    private @Id UUID player;
    private long discord;

    public Player() {
    }

    public Player(UUID player, long discord) {
        this.player = player;
        this.discord = discord;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public long getDiscord() {
        return discord;
    }

    public void setDiscord(long discord) {
        this.discord = discord;
    }
}
