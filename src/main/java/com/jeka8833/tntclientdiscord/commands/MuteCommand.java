package com.jeka8833.tntclientdiscord.commands;

import com.jeka8833.tntclientdiscord.MessageUtil;
import com.jeka8833.tntclientdiscord.TNTChatManager;
import com.jeka8833.tntclientdiscord.api.MojangPlayer;
import com.jeka8833.tntclientdiscord.api.websocket.packet.packets.MutePacket;
import com.jeka8833.tntclientdiscord.database.DiscordUserConfigurationRepository;
import com.jeka8833.tntclientdiscord.database.Muted;
import com.jeka8833.tntclientdiscord.database.MutedRepository;
import com.jeka8833.tntclientdiscord.listeners.SlashCommandListener;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class MuteCommand implements SlashCommandListener {
    private static final Map<Pattern, TimeUnit> regexToTimeUnit = Map.of(
            Pattern.compile("^(\\d+)s$"), TimeUnit.SECONDS,
            Pattern.compile("^(\\d+)m$"), TimeUnit.MINUTES,
            Pattern.compile("^(\\d+)h$"), TimeUnit.HOURS,
            Pattern.compile("^(\\d+)d$"), TimeUnit.DAYS
    );
    private static final Logger logger = LoggerFactory.getLogger(MuteCommand.class);

    private final DiscordUserConfigurationRepository configurationRepository;
    private final MutedRepository mutedRepository;
    private final GatewayDiscordClient client;


    public MuteCommand(DiscordUserConfigurationRepository configurationRepository,
                       MutedRepository mutedRepository, GatewayDiscordClient client) {
        this.configurationRepository = configurationRepository;
        this.mutedRepository = mutedRepository;
        this.client = client;
    }

    @Override
    public String getName() {
        return "mute";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        long userID = event.getInteraction().getUser().getId().asLong();

        if (!configurationRepository.hasRole(userID, "MUTE")) {
            return MessageUtil.printYouDontHavePermission(event, userID);
        }

        Optional<ApplicationCommandInteractionOption> addCommand = event.getOption("add");
        Optional<ApplicationCommandInteractionOption> removeCommand = event.getOption("remove");
        Optional<ApplicationCommandInteractionOption> listCommand = event.getOption("list");

        if (addCommand.isPresent()) {
            addCommand(event, userID, addCommand.get());
        } else if (removeCommand.isPresent()) {
            removeCommand(event, userID, removeCommand.get());
        } else if (listCommand.isPresent()) {
            listCommand(event);
        } else {
            return MessageUtil.printErrorMessage(event, "Invalid command usage.");
        }

        return event.deferReply().withEphemeral(true);
    }

    private void addCommand(ChatInputInteractionEvent event, long userID,
                            ApplicationCommandInteractionOption subcommand) {
        String player = subcommand.getOption("player")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        String description = subcommand.getOption("description")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        String time = subcommand.getOption("time")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        Mono.defer(() -> {
            try {
                Duration duration = parseTime(time);

                if (duration == null) {
                    return event.editReply()
                            .withEmbeds(EmbedCreateSpec.builder()
                                    .color(Color.RED)
                                    .title("Invalid time format.")
                                    .build());
                }

                MojangPlayer mojangPlayer = MojangPlayer.parse(player);

                if (mojangPlayer == null || mojangPlayer.getUuidOrRequest().blockOptional().isEmpty()) {
                    return event.editReply()
                            .withEmbeds(EmbedCreateSpec.builder()
                                    .color(Color.RED)
                                    .title("Player not found or MojangAPI is down.")
                                    .build());
                }

                String descriptionFixed = description.substring(0, Math.min(1024, description.length()));

                var mutedUsers = new Muted();
                mutedUsers.setModerator(userID);
                mutedUsers.setPlayer(mojangPlayer.getUUID());
                mutedUsers.setReason(descriptionFixed);
                mutedUsers.setUnmuteTime(ZonedDateTime.now().plus(duration));
                mutedRepository.save(mutedUsers);

                MutePacket.sentLastedList();

                TNTChatManager.log(userID, "Muted player " + mojangPlayer + " until " +
                        mutedUsers.getUnmuteTime() + " for the reason: " + descriptionFixed);

                return event.editReply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.GREEN)
                                .title("Operation completed")
                                .description("Player " + mojangPlayer + " muted until " +
                                        mutedUsers.getUnmuteTime() + " for the reason: " + descriptionFixed + ".")
                                .build());
            } catch (Exception e) {
                logger.warn("Mute add command error", e);

                return event.editReply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .title("Operation failed")
                                .build());
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private void removeCommand(ChatInputInteractionEvent event, long userID,
                               ApplicationCommandInteractionOption subcommand) {
        String player = subcommand.getOption("player")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        Mono.defer(() -> {
            try {
                MojangPlayer mojangPlayer = MojangPlayer.parse(player);

                if (mojangPlayer == null || mojangPlayer.getUuidOrRequest().blockOptional().isEmpty()) {
                    return event.editReply()
                            .withEmbeds(EmbedCreateSpec.builder()
                                    .color(Color.RED)
                                    .title("Player not found or MojangAPI is down.")
                                    .build());
                }

                mutedRepository.deleteById(Objects.requireNonNull(mojangPlayer.getUUID()));

                MutePacket.sentLastedList();

                TNTChatManager.log(userID, "Player " + mojangPlayer + " has been unmuted");

                return event.editReply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.GREEN)
                                .title("Operation completed")
                                .description("Player " + mojangPlayer + " has been unmuted")
                                .build());
            } catch (Exception e) {
                logger.warn("Mute remove command error", e);

                return event.editReply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .title("Operation failed")
                                .build());
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private void listCommand(ChatInputInteractionEvent event) {
        Mono.defer(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (Muted mutedUser : mutedRepository.findAll()) {
                    if (!mutedUser.isMuted()) continue;

                    try {
                        Optional<Optional<String>> moderatorNameOptional =
                                client.getUserById(Snowflake.of(mutedUser.getModerator()))
                                        .map(User::getGlobalName).blockOptional();
                        String moderatorName = moderatorNameOptional.map(s -> s.orElse("")).orElse("");
                        moderatorName = moderatorName.isEmpty() ? "" : '(' + moderatorName + ')';

                        var playerInformation = new MojangPlayer(mutedUser.getPlayer());
                        sb.append("Moderator ").append(mutedUser.getModerator()).append(moderatorName)
                                .append(" blocked ").append(playerInformation)
                                .append(" until ").append(mutedUser.getUnmuteTime())
                                .append(" for the reason: ").append(mutedUser.getReason()).append("\n\n");
                    } catch (Exception e) {
                        logger.warn("Mute list command error", e);

                        sb.append("Moderator ").append(mutedUser.getModerator())
                                .append(" blocked ").append(mutedUser.getPlayer())
                                .append(" until ").append(mutedUser.getUnmuteTime())
                                .append(" for the reason: ").append(mutedUser.getReason()).append("\n\n");
                    }
                }

                return event.editReply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.GREEN)
                                .title("Player mute list")
                                .description(sb.isEmpty() ? "Empty" : sb.toString())
                                .build());
            } catch (Exception e) {
                logger.warn("Mute list command error", e);

                return event.editReply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(Color.RED)
                                .title("Operation failed")
                                .build());
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Nullable
    @Contract(pure = true)
    private static Duration parseTime(@NotNull String time) {
        try {
            Duration duration = Duration.parse(time);

            if (duration.isNegative() || duration.isZero()) return null;

            return duration;
        } catch (Exception ignored) {
        }

        try {
            String[] args = time.strip().toLowerCase().split(" ");

            long seconds = 0;
            for (String arg : args) {
                for (Map.Entry<Pattern, TimeUnit> entry : regexToTimeUnit.entrySet()) {
                    if (entry.getKey().matcher(arg).matches()) {
                        seconds += entry.getValue().toSeconds(Long.parseLong(arg.substring(0, arg.length() - 1)));
                    }
                }
            }

            if (seconds <= 0) return null;

            return Duration.ofSeconds(seconds);
        } catch (Exception ignored) {
        }

        return null;
    }
}
