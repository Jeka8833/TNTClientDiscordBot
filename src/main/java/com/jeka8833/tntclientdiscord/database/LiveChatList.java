package com.jeka8833.tntclientdiscord.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "tnd_live_chat_list")
public class LiveChatList {
    @Id
    private long chatID;

    private long userID;

    public long getChatID() {
        return chatID;
    }

    public void setChatID(long chatID) {
        this.chatID = chatID;
    }

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }
}
