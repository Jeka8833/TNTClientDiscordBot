package com.jeka8833.tntclientdiscord;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class MessageUtil {

    public static void sendPrivateMessage(long userID, String message) {
        StaticContextAccessor.getBean(GatewayDiscordClient.class)
                .getUserById(Snowflake.of(userID))
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> channel.createMessage(
                        EmbedCreateSpec.builder()
                                .color(Color.GREEN)
                                .description(message)
                                .build()))
                .retry(3)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    public static Mono<Void> printYouDontHavePermission(ChatInputInteractionEvent event, long userID) {
        return printErrorMessage(event, "You don't have permission to use this command. User ID: " + userID);
    }

    public static Mono<Void> printErrorMessage(ChatInputInteractionEvent event, String message) {
        return event.reply()
                .withEphemeral(true)
                .withEmbeds(
                        EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .description(message)
                                .build()
                );
    }

    public static Mono<Void> printGoodMessage(ChatInputInteractionEvent event, String message) {
        return event.reply()
                .withEphemeral(true)
                .withEmbeds(
                        EmbedCreateSpec.builder()
                                .color(Color.GREEN)
                                .description(message)
                                .build()
                );
    }
}
