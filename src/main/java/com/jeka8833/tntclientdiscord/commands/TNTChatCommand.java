package com.jeka8833.tntclientdiscord.commands;

import com.jeka8833.tntclientdiscord.MessageUtil;
import com.jeka8833.tntclientdiscord.database.DiscordUserConfigurationRepository;
import com.jeka8833.tntclientdiscord.database.LiveChatList;
import com.jeka8833.tntclientdiscord.database.LiveChatListRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class TNTChatCommand implements SlashCommand {
    private static final Logger logger = LoggerFactory.getLogger(TNTChatCommand.class);

    private final DiscordUserConfigurationRepository configurationRepository;
    private final LiveChatListRepository liveChatListRepository;

    public TNTChatCommand(DiscordUserConfigurationRepository configurationRepository,
                          LiveChatListRepository liveChatListRepository) {
        this.configurationRepository = configurationRepository;
        this.liveChatListRepository = liveChatListRepository;
    }

    @Override
    public String getName() {
        return "tntchat";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        long userID = event.getInteraction().getUser().getId().asLong();

        if (!configurationRepository.hasRole(userID, "TNTCLIENT_CHAT")) {
            return MessageUtil.printYouDontHavePermission(event, userID);
        }

        Optional<ApplicationCommandInteractionOption> connectCommand = event.getOption("connect");
        Optional<ApplicationCommandInteractionOption> disconnectCommand = event.getOption("disconnect");

        if (connectCommand.isPresent()) {
            return connectCommand(event, userID);
        } else if (disconnectCommand.isPresent()) {
            return disconnectCommand(event, disconnectCommand.get());
        }

        return MessageUtil.printErrorMessage(event, "Invalid command usage.");
    }

    private Mono<Void> connectCommand(ChatInputInteractionEvent event, long userID) {
        try {
            long chatID = event.getInteraction().getChannelId().asLong();

            var chatList = new LiveChatList();
            chatList.setChatID(chatID);
            chatList.setUserID(userID);
            liveChatListRepository.save(chatList);

            return MessageUtil.printGoodMessage(event, "Successfully connected to the chat.");
        } catch (Exception e) {
            logger.warn("Error while connecting to the chat: ", e);

            return MessageUtil.printErrorMessage(event, "An error occurred while connecting to the chat.");
        }
    }

    private Mono<Void> disconnectCommand(ChatInputInteractionEvent event,
                                         ApplicationCommandInteractionOption subcommand) {
        try {
            Optional<String> customChatID = subcommand.getOption("channel")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString);

            if (customChatID.isPresent()) {
                try {
                    long chatID = Long.parseLong(customChatID.get());

                    liveChatListRepository.deleteById(chatID);

                    return MessageUtil.printGoodMessage(event, "Successfully disconnected from the chat.");
                } catch (Exception e) {
                    logger.warn("Error while disconnecting from the chat: ", e);

                    return MessageUtil.printErrorMessage(event,
                            "Invalid channel ID. Please, make sure you entered the correct ID.");
                }
            }


            long chatID = event.getInteraction().getChannelId().asLong();

            liveChatListRepository.deleteById(chatID);

            return MessageUtil.printGoodMessage(event, "Successfully disconnected from the chat.");
        } catch (Exception e) {
            logger.warn("Error while disconnecting from the chat: ", e);

            return MessageUtil.printErrorMessage(event, "An error occurred while disconnecting from the chat.");
        }
    }
}
