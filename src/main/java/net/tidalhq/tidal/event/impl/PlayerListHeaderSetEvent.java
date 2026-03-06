package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;

public class PlayerListHeaderSetEvent implements Event {
    private final String headerContent;
    public PlayerListHeaderSetEvent(String headerContent) {
        this.headerContent = headerContent;
    }

    public String getHeaderContent() {
        return headerContent;
    }
}
