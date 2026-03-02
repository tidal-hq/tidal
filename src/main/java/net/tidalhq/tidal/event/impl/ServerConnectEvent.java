package net.tidalhq.tidal.event.impl;

import net.minecraft.client.network.ServerInfo;
import net.tidalhq.tidal.event.Event;

public class ServerConnectEvent implements Event {
    private final ServerInfo serverInfo;

    public ServerConnectEvent(ServerInfo joined) {
        this.serverInfo = joined;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}
