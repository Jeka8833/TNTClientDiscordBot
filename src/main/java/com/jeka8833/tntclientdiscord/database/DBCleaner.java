package com.jeka8833.tntclientdiscord.database;

import com.jeka8833.tntclientdiscord.api.websocket.packet.packets.MutePacket;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
public class DBCleaner {

    private final MutedRepository mutedRepository;

    public DBCleaner(MutedRepository mutedRepository) {
        this.mutedRepository = mutedRepository;
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void clearOld() {
        boolean isChanged = false;
        for (Muted muted : mutedRepository.findAll()) {
            if (!muted.isMuted()) {
                mutedRepository.delete(muted);
                isChanged = true;
            }
        }

        if (isChanged) MutePacket.sentLastedList();
    }
}
