package net.tidalhq.tidal.event.impl;

import net.minecraft.client.network.PlayerListEntry;
import net.tidalhq.tidal.event.Event;

import java.util.Collection;

public class PlayerListUpdateEvent implements Event {
    private final Collection<PlayerListEntry> entries;

    public PlayerListUpdateEvent(Collection<PlayerListEntry> entries) {
        this.entries = entries;
    }

    public Collection<PlayerListEntry> getEntries() {
        return entries;
    }
}
