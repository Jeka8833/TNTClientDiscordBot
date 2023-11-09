package com.jeka8833.tntclientdiscord.database;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscordUserConfigurationRepository extends JpaRepository<DiscordUserConfiguration, Long> {
    default boolean hasRole(long discordID, String role) {
        return findById(discordID)
                .map(discordUserConfiguration -> discordUserConfiguration.hasRole(role))
                .orElse(false);

    }
}
