package net.tidalhq.tidal.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.Nullable;

public class ServerState {
    private static ServerState instance;

    private String currentServerAddress;
    private String currentServerName;
    private boolean isConnectedToServer;
    private long lastServerCheckTime;

    private ServerState() {
        this.currentServerAddress = null;
        this.currentServerName = null;
        this.isConnectedToServer = false;
        this.lastServerCheckTime = 0;
    }

    public static ServerState getInstance() {
        if (instance == null) {
            instance = new ServerState();
        }
        return instance;
    }

    public void update() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
            this.isConnectedToServer = true;

            ServerInfo serverInfo = client.getCurrentServerEntry();

            if (serverInfo != null) {
                this.currentServerAddress = serverInfo.address;
                this.currentServerName = serverInfo.name;
            } else if (client.isIntegratedServerRunning()) {
                this.currentServerAddress = "singleplayer";
                this.currentServerName = client.getServer() != null ?
                        client.getServer().getSaveProperties().getLevelName() : "Singleplayer";
            }
        } else {
            this.isConnectedToServer = false;
            this.currentServerAddress = null;
            this.currentServerName = null;
        }

        this.lastServerCheckTime = System.currentTimeMillis();
    }

    @Nullable
    public String getCurrentServerAddress() {
        return currentServerAddress;
    }

    @Nullable
    public String getCurrentServerName() {
        return currentServerName;
    }

    public boolean isConnectedToServer() {
        return isConnectedToServer;
    }

    public boolean isConnectedToMultiplayer() {
        return isConnectedToServer &&
                currentServerAddress != null &&
                !currentServerAddress.equals("singleplayer");
    }

    public boolean isInSinglePlayer() {
        return isConnectedToServer &&
                currentServerAddress != null &&
                currentServerAddress.equals("singleplayer");
    }

    public boolean isOnServer(String address) {
        if (!isConnectedToServer || currentServerAddress == null) {
            return false;
        }

        return currentServerAddress.equals(address) ||
                currentServerAddress.startsWith(address + ":");
    }

    public long getLastServerCheckTime() {
        return lastServerCheckTime;
    }

    public void reset() {
        this.currentServerAddress = null;
        this.currentServerName = null;
        this.isConnectedToServer = false;
        this.lastServerCheckTime = System.currentTimeMillis();
    }
}