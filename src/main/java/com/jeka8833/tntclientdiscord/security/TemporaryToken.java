package com.jeka8833.tntclientdiscord.security;

import java.time.LocalDateTime;

public record TemporaryToken(long discordId, LocalDateTime expireTime) {
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemporaryToken that = (TemporaryToken) o;

        return discordId == that.discordId;
    }

    @Override
    public int hashCode() {
        return (int) (discordId ^ (discordId >>> 32));
    }
}
