package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;

public class PlayerListFooterSetEvent implements Event {
    private final String footerContent;
    public PlayerListFooterSetEvent(String footerContent) {
        this.footerContent = footerContent;
    }

    public String getFooterContent() {
        return footerContent;
    }
}
