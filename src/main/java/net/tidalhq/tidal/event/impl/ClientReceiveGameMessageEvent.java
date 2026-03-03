package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;

public class ClientReceiveGameMessageEvent implements Event {
    private final String messageContent;

    public ClientReceiveGameMessageEvent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getMessageContent() {
        return messageContent;
    }
}