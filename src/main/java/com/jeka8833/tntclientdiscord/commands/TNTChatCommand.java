package com.jeka8833.tntclientdiscord.commands;

import com.jeka8833.tntclientdiscord.MessageUtil;
import com.jeka8833.tntclientdiscord.ServerType;
import com.jeka8833.tntclientdiscord.TNTChatManager;
import com.jeka8833.tntclientdiscord.api.MojangPlayer;
import com.jeka8833.tntclientdiscord.api.websocket.packet.packets.ChatHookPacket;
import com.jeka8833.tntclientdiscord.database.*;
import com.jeka8833.tntclientdiscord.listeners.ButtonListener;
import com.jeka8833.tntclientdiscord.listeners.SlashCommandListener;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
public class TNTChatCommand implements SlashCommandListener, ButtonListener {
    private static final UUID UNKNOWN_USER = new UUID(0, 0);

    private static final Logger LOGGER = LoggerFactory.getLogger(TNTChatCommand.class);
    private static final Map<String, WaitingMessage> WAITING_MESSAGE_MAP = new ConcurrentHashMap<>();


    private final DiscordUserConfigurationRepository configurationRepository;
    private final LiveChatListRepository liveChatListRepository;
    private final PlayerRepository playerRepository;

    public TNTChatCommand(DiscordUserConfigurationRepository configurationRepository,
                          LiveChatListRepository liveChatListRepository, PlayerRepository playerRepository) {
        this.configurationRepository = configurationRepository;
        this.liveChatListRepository = liveChatListRepository;
        this.playerRepository = playerRepository;
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void checkTimeout() {
        WAITING_MESSAGE_MAP.values().removeIf(WaitingMessage::isTimeout);
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event) {
        WaitingMessage waitingMessage = WAITING_MESSAGE_MAP.remove(event.getCustomId());
        if (waitingMessage != null) {
            if (waitingMessage.isTimeout()) {
                return event.edit()
                        .withComponents()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .description("Timeout")
                                .build());
            }

            return waitingMessage.send(event);
        } else {
            return Mono.empty();
        }
    }

    @Override
    public String getName() {
        return "tntchat";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        long userID = event.getInteraction().getUser().getId().asLong();

        Optional<ApplicationCommandInteractionOption> connectCommand = event.getOption("connect");
        Optional<ApplicationCommandInteractionOption> disconnectCommand = event.getOption("disconnect");
        Optional<ApplicationCommandInteractionOption> sendCommand = event.getOption("send");

        if (connectCommand.isPresent()) {
            if (!configurationRepository.hasRole(userID, "TNTCLIENT_CHAT_CONNECT")) {
                return MessageUtil.printYouDontHavePermission(event, userID);
            }

            return connectCommand(event, userID);
        } else if (disconnectCommand.isPresent()) {
            if (!configurationRepository.hasRole(userID, "TNTCLIENT_CHAT_CONNECT")) {
                return MessageUtil.printYouDontHavePermission(event, userID);
            }

            return disconnectCommand(event, disconnectCommand.get());
        } else if (sendCommand.isPresent()) {
            if (!configurationRepository.hasRole(userID, "TNTCLIENT_CHAT_SEND")) {
                return MessageUtil.printYouDontHavePermission(event, userID);
            }

            return sendCommand(event, userID, sendCommand.get());
        }

        return MessageUtil.printErrorMessage(event, "Invalid command.");
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
            LOGGER.warn("Error while connecting to the chat: ", e);

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
                    LOGGER.warn("Error while disconnecting from the chat: ", e);

                    return MessageUtil.printErrorMessage(event,
                            "Invalid channel ID. Please, make sure you entered the correct ID.");
                }
            }


            long chatID = event.getInteraction().getChannelId().asLong();

            liveChatListRepository.deleteById(chatID);

            return MessageUtil.printGoodMessage(event, "Successfully disconnected from the chat.");
        } catch (Exception e) {
            LOGGER.warn("Error while disconnecting from the chat: ", e);

            return MessageUtil.printErrorMessage(event, "An error occurred while disconnecting from the chat.");
        }
    }

    private Mono<Void> sendCommand(ChatInputInteractionEvent event, long userID,
                                   ApplicationCommandInteractionOption subcommand) {
        String message = subcommand.getOption("message")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).get();
        Optional<String> receiverOpt = subcommand.getOption("receiver")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);
        Optional<String> serverOpt = subcommand.getOption("server")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        Mono.defer(() -> {
            try {
                UUID receiver;
                if (receiverOpt.isPresent()) {
                    MojangPlayer receiverInfo = MojangPlayer.parse(receiverOpt.get());
                    if (receiverInfo == null) {
                        return event.editReply()
                                .withEmbeds(EmbedCreateSpec.builder()
                                        .color(Color.RED)
                                        .title("Invalid player name or UUID.")
                                        .build());
                    }

                    Optional<UUID> receiverResult = receiverInfo.getUuidOrRequest().blockOptional();
                    if (receiverResult.isEmpty()) {
                        return event.editReply()
                                .withEmbeds(EmbedCreateSpec.builder()
                                        .color(Color.RED)
                                        .title("Player not found or MojangAPI is down.")
                                        .build());
                    }
                    receiver = receiverResult.get();
                } else {
                    receiver = null;
                }

                ServerType server = serverOpt.map(ServerType::getServer).orElse(ServerType.UNKNOWN);


                List<Button> buttons = new ArrayList<>();
                Collection<Player> senderAccounts = playerRepository.findByDiscord(userID);
                for (Player player : senderAccounts) {
                    var mojangPlayer = new MojangPlayer(player.getPlayer());
                    var waitingMessage = new WaitingMessage(player.getPlayer(), receiver, server, message);

                    UUID randomID = UUID.randomUUID();
                    buttons.add(Button.primary(randomID.toString(), mojangPlayer.toString()));
                    WAITING_MESSAGE_MAP.put(randomID.toString(), waitingMessage);
                }

                var waitingMessage = new WaitingMessage(UNKNOWN_USER, receiver, server, message);

                UUID randomID = UUID.randomUUID();
                buttons.add(Button.primary(randomID.toString(), "Unknown User"));
                WAITING_MESSAGE_MAP.put(randomID.toString(), waitingMessage);


                return event.editReply()
                        .withComponents(ActionRow.of(buttons))
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.BLUE)
                                .description("Please select the account from which you wish to send the message.")
                                .build());
            } catch (Exception e) {
                LOGGER.warn("Send message error", e);

                return event.editReply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .title("Operation failed")
                                .build());
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();

        return event.deferReply().withEphemeral(true);
    }

    private record WaitingMessage(long timeoutAt, @NotNull UUID sender, @Nullable UUID receiver,
                                  @NotNull ServerType serverType, @NotNull String message) {

        private static final long TIMEOUT = 10 * 60 * 1000;

        private WaitingMessage(@NotNull UUID sender, @Nullable UUID receiver,
                               @NotNull ServerType serverType, @NotNull String message) {
            this(System.currentTimeMillis() + TIMEOUT, sender, receiver, serverType, message);
        }

        private boolean isTimeout() {
            return System.currentTimeMillis() > timeoutAt;
        }

        @NotNull
        private Mono<Void> send(@NotNull ButtonInteractionEvent event) {
            ChatHookPacket.sendMessage(sender, receiver, serverType, message);

            if (sender.equals(UNKNOWN_USER)) {
                TNTChatManager.log(event.getInteraction().getUser().getId().asLong(),
                        "Used unknown user to send message.");
            }

            return event.edit()
                    .withComponents()
                    .withEmbeds(EmbedCreateSpec.builder()
                            .color(Color.GREEN)
                            .description("Message sent successfully.")
                            .build());
        }
    }
}
