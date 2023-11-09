package com.jeka8833.tntclientdiscord.security;

import com.jeka8833.tntclientdiscord.database.Player;
import com.jeka8833.tntclientdiscord.database.PlayerRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
public class TokenManager {
    private static final Random random = new SecureRandom();

    private final Map<Integer, TemporaryToken> codeToToken = new ConcurrentHashMap<>();

    private final PlayerRepository playerRepository;

    public TokenManager(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public int generateToken(long discordId) {
        int token;
        do {
            token = random.nextInt(1000000); // 0 - 999 999
        } while (codeToToken.containsKey(token));

        codeToToken.values().removeIf(temporaryToken -> temporaryToken.discordId() == discordId);
        codeToToken.put(token, new TemporaryToken(discordId, LocalDateTime.now().plusMinutes(1)));

        return token;
    }

    public Optional<Long> removeLink(UUID player) {
        Optional<Player> playerInDB = playerRepository.findById(player);
        playerRepository.deleteById(player);

        return playerInDB.map(Player::getDiscord);

    }

    public Optional<Long> validateToken(UUID player, int token) {
        TemporaryToken user = codeToToken.remove(token);
        if (user == null || user.isExpired()) return Optional.empty();

        Player playerRow = new Player(player, user.discordId());
        playerRepository.save(playerRow);

        return Optional.of(user.discordId());
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void clearOldTokens() {
        codeToToken.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
