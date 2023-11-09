package com.jeka8833.tntclientdiscord.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Set;

@Entity(name = "tnd_discord_users")
public class DiscordUserConfiguration {

    private static final String DETONATOR = ",";

    @Id
    private long discordID;
    private String roles;

    public long getDiscordID() {
        return discordID;
    }

    public void setDiscordID(long discordID) {
        this.discordID = discordID;
    }

    public Set<String> getRoles() {
        return Set.of(roles.split(DETONATOR));
    }

    public void setRoles(Set<String> roles) {
        this.roles = String.join(DETONATOR, roles.toArray(new String[0]));
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }
}
