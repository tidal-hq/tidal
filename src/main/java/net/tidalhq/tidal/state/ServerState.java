package net.tidalhq.tidal.state;

import net.minecraft.client.network.ServerInfo;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ServerConnectEvent;
import net.tidalhq.tidal.event.impl.ServerDisconnectEvent;

public class ServerState {

    private ServerInfo serverInfo;

    public ServerState(EventBus eventBus) {
        eventBus.register(this);
    }

    @Subscribe
    public void onServerConnect(ServerConnectEvent event) {
        this.serverInfo = event.getServerInfo();
    }

    @Subscribe
    public void onServerDisconnect(ServerDisconnectEvent event) {
        this.serverInfo = null;
    }

    public boolean isConnectedToHypixel() {
        return serverInfo != null
                && serverInfo.address.toLowerCase().contains("hypixel.net");
    }
}