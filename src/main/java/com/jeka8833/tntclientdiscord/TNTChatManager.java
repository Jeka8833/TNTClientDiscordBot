package com.jeka8833.tntclientdiscord;

import com.jeka8833.tntclientdiscord.api.MojangPlayer;
import com.jeka8833.tntclientdiscord.database.LiveChatList;
import com.jeka8833.tntclientdiscord.database.LiveChatListRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.EmbedAuthorData;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFooterData;
import discord4j.discordjson.json.ImmutableEmbedFooterData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TNTChatManager {
    private static final Logger logger = LogManager.getLogger(TNTChatManager.class);
    private static final LiveChatListRepository chatList =
            StaticContextAccessor.getBean(LiveChatListRepository.class);
    private static final GatewayDiscordClient discordClient =
            StaticContextAccessor.getBean(GatewayDiscordClient.class);

    public static void sendMessage(@NotNull UUID sender, @Nullable UUID receiver,
                                   @NotNull ServerType serverType, @NotNull String message) {
        Mono.fromRunnable(() -> {
            var senderAccount = new MojangPlayer(sender);

            String server = serverType == ServerType.UNKNOWN ?
                    "Server: Global" : "Server: " + serverType.getDisplayName();

            ImmutableEmbedFooterData footerBuilder;
            if (receiver != null) {
                var receiverAccount = new MojangPlayer(receiver);

                footerBuilder = EmbedFooterData.builder()
                        .iconUrl("https://mc-heads.net/avatar/" + receiver)
                        .text("Dirrect to: " + receiverAccount + "; " + server)
                        .build();
            } else {
                footerBuilder = EmbedFooterData.builder()
                        .text(server)
                        .build();
            }


            EmbedData embed = EmbedData.builder()
                    .color(Color.CYAN.getRGB())
                    .author(EmbedAuthorData.builder()
                            .name(senderAccount.toString())
                            .iconUrl("https://mc-heads.net/avatar/" + sender)
                            .build())
                    .description(message)
                    .footer(footerBuilder)
                    .timestamp(Instant.now().toString())
                    .build();

            send(embed);
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    public static void log(long userID, String message) {
        Mono.fromRunnable(() -> {
            Optional<User> discordUser = discordClient.getUserById(Snowflake.of(userID)).blockOptional();

            EmbedData embed = EmbedData.builder()
                    .color(Color.YELLOW.getRGB())
                    .author(EmbedAuthorData.builder()
                            .name(discordUser
                                    .flatMap(User::getGlobalName)
                                    .orElse("Unknown") + " (" + userID + ")")
                            .iconUrl(discordUser
                                    .map(user -> Possible.of(user.getAvatarUrl()))
                                    .orElse(Possible.absent()))
                            .build())
                    .description(message)
                    .build();

            send(embed);
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private static void send(EmbedData embedData) {
        for (LiveChatList chat : chatList.findAll()) {
            try {
                discordClient.getChannelById(Snowflake.of(chat.getChatID()))
                        .flatMap(channel -> channel.getRestChannel().createMessage(embedData))
                        .subscribe();
            } catch (Exception e) {
                logger.error("Error while sending message to discord", e);
            }
        }
    }
}
