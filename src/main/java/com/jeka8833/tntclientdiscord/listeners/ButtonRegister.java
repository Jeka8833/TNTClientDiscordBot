package com.jeka8833.tntclientdiscord.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
public class ButtonRegister {
    private final Collection<ButtonListener> buttons;

    public ButtonRegister(List<ButtonListener> buttons, GatewayDiscordClient client) {
        this.buttons = buttons;

        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(ButtonInteractionEvent event) {
        return Flux.fromIterable(buttons)
                .flatMap(buttonListener -> buttonListener.handle(event))
                .then();
    }
}
