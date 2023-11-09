package com.jeka8833.tntclientdiscord.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity(name = "tnd_muted_users")
public class Muted {
    @Id
    private UUID player;
    private long moderator;
    private String reason;
    private ZonedDateTime unmuteTime;

    public UUID getPlayer() {
        return player;
    }

    public long getModerator() {
        return moderator;
    }

    public String getReason() {
        return reason;
    }

    public ZonedDateTime getUnmuteTime() {
        return unmuteTime;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public void setModerator(long moderator) {
        this.moderator = moderator;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setUnmuteTime(ZonedDateTime unmuteTime) {
        this.unmuteTime = unmuteTime;
    }

    public boolean isMuted() {
        return ZonedDateTime.now().isBefore(unmuteTime);
    }
}
