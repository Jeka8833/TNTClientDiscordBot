package com.jeka8833.tntclientdiscord.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeka8833.tntclientdiscord.Main;
import okhttp3.*;
import org.jetbrains.annotations.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class MojangPlayer {
    private static final long CACHE_TIME = 10 * 60 * 1000;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    private static final ObjectMapper json = new ObjectMapper();
    static final Map<UUID, MojangPlayer> playerNameCache = new ConcurrentHashMap<>();

    @Nullable
    private UUID uuid;
    @Nullable
    private String name;
    private long deleteAt;

    public MojangPlayer(@NotNull UUID uuid) {
        this(uuid, null);
    }

    public MojangPlayer(@NotNull String name) {
        this(null, name);
    }

    @Contract("null, null -> fail")
    public MojangPlayer(@Nullable UUID uuid, @Nullable String name) {
        if (uuid == null && name == null) {
            throw new IllegalArgumentException("UUID and name cannot be null at the same time.");
        }

        this.uuid = uuid;
        this.name = name;

        use();
    }

    private void use() {
        deleteAt = System.currentTimeMillis() + CACHE_TIME;
    }

    private boolean isExpired() {
        return deleteAt < System.currentTimeMillis();
    }

    @Nullable
    public UUID getUUID() {
        return uuid;
    }

    @NotNull
    @Contract(value = "-> new", pure = true)
    public Mono<UUID> getUuidOrRequest() {
        if (uuid == null) {
            return Mono.<UUID>create(sink -> getPlayerUUID(name, uuid -> {
                        if (uuid.isPresent())
                            sink.success(uuid.get().getUUID());
                        else
                            sink.success();
                    }))
                    .doOnNext(uuid -> this.uuid = uuid);
        }

        return Mono.just(uuid);
    }

    @Nullable
    public String getName() {
        return name;
    }

    @NotNull
    @Contract(value = "-> new", pure = true)
    public Mono<String> getNameOrRequest() {
        if (name == null) {
            return Mono.<String>create(sink ->
                            getPlayerName(uuid, username -> {
                                if (username.isPresent())
                                    sink.success(username.get().getName());
                                else
                                    sink.success();
                            }))
                    .doOnNext(name -> this.name = name);
        }

        return Mono.just(name);
    }

    @Blocking
    @Override
    public String toString() {
        Optional<UUID> uuid = getUuidOrRequest().blockOptional();
        Optional<String> username = getNameOrRequest().blockOptional();

        if (username.isPresent() && uuid.isPresent()) {
            return username.get() + " (" + uuid.get() + ")";
        } else if (username.isPresent()) {
            return username.get();
        } else if (uuid.isPresent()) {
            return uuid.get().toString();
        } else {
            return "Unknown user";
        }
    }

    @Nullable
    @Contract(pure = true)
    public static MojangPlayer parse(@NotNull String player) {
        try {
            if (UUID_V4_PATTERN.matcher(player).matches()) {
                return new MojangPlayer(UUID.fromString(player));
            } else if (USERNAME_PATTERN.matcher(player).matches()) {
                return new MojangPlayer(player);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @NonBlocking
    public static void getPlayerName(@Nullable UUID uuid,
                                     @NotNull Consumer<@NotNull Optional<@NotNull MojangPlayer>> listener) {
        if (uuid == null || uuid.version() != 4 || uuid.variant() != 2) {
            listener.accept(Optional.empty());
            return;
        }

        MojangPlayer playerCache = playerNameCache.get(uuid);
        if (playerCache != null) {
            listener.accept(Optional.of(playerCache));
            playerCache.use();
            return;
        }

        Main.okHttpClient.newCall(new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/profile/" +
                        uuid.toString().replace("-", ""))
                .build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        try (Reader reader = responseBody.charStream()) {
                            Player player = json.readValue(reader, Player.class);
                            if (player != null && player.name != null) {
                                var playerInformation = new MojangPlayer(uuid, player.name);

                                listener.accept(Optional.of(playerInformation));

                                playerNameCache.put(uuid, playerInformation);
                                return;
                            }
                        }
                    }
                }
                listener.accept(Optional.empty());
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.accept(Optional.empty());
            }
        });
    }

    @NonBlocking
    public static void getPlayerUUID(@Nullable String name,
                                     @NotNull Consumer<@NotNull Optional<@NotNull MojangPlayer>> listener) {
        if (name == null) {
            listener.accept(Optional.empty());
            return;
        }

        for (MojangPlayer nameCache : playerNameCache.values()) {
            if (name.equalsIgnoreCase(nameCache.name)) {
                listener.accept(Optional.of(nameCache));
                nameCache.use();
                return;
            }
        }

        Main.okHttpClient.newCall(new Request.Builder()
                .url("https://api.mojang.com/users/profiles/minecraft/" + name)
                .build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        try (Reader reader = responseBody.charStream()) {
                            Player player = json.readValue(reader, Player.class);
                            if (player != null) {
                                UUID playerUUID = player.getUUID();
                                if (playerUUID != null) {
                                    var playerInformation = new MojangPlayer(playerUUID, player.getUsername());

                                    listener.accept(Optional.of(playerInformation));

                                    playerNameCache.put(playerUUID, playerInformation);
                                    return;
                                }
                            }
                        }
                    }
                }

                listener.accept(Optional.empty());
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.accept(Optional.empty());
            }
        });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Player {
        public String name;
        public String id;

        @Nullable
        private String getUsername() {
            return name;
        }

        @Nullable
        private UUID getUUID() {
            if (id == null) return null;

            return UUID.fromString(
                    id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        }
    }

    @Component
    @EnableScheduling
    public record CacheCleaner() {
        @Scheduled(fixedDelay = MojangPlayer.CACHE_TIME)
        public void clearOldCache() {
            MojangPlayer.playerNameCache.values().removeIf(MojangPlayer::isExpired);
        }
    }
}
