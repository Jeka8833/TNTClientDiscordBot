package com.jeka8833.tntclientdiscord.commands;

import com.jeka8833.tntclientdiscord.MessageUtil;
import com.jeka8833.tntclientdiscord.listeners.SlashCommandListener;
import com.jeka8833.tntclientdiscord.security.TokenManager;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RegisterCommand implements SlashCommandListener {

    private final TokenManager tokenManager;

    public RegisterCommand(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        long userID = event.getInteraction().getUser().getId().asLong();


        //Reply to the slash command, with the name the user supplied
        return MessageUtil.printGoodMessage(event,
                "Write the command \"@discordlink " + tokenManager.generateToken(userID) +
                        "\" into the game chat using TNTClient. You have 1 minute to do this," +
                        " if you have not done it in time, generate the token again.");
    }
}
