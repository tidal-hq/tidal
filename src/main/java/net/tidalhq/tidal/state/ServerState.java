package net.tidalhq.tidal.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ServerConnectEvent;
import net.tidalhq.tidal.event.impl.ServerDisconnectEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerState {
    private static final ServerState instance = new ServerState();

    private ServerInfo serverInfo;

    private ServerState() {
        this.serverInfo = null;

        EventBus.getInstance().register(this);
    }

    public static ServerState getInstance() {
        return instance;
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
        if (this.serverInfo == null) return false;

        return this.serverInfo.address.toLowerCase().contains("hypixel.net");
    }

}