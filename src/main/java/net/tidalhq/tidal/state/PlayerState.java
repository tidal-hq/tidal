package net.tidalhq.tidal.state;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class PlayerState {
    private static PlayerState instance;

    public static PlayerState getInstance() {
        if (instance == null) {
            instance = new PlayerState();
            instance.init();
        }
        return instance;
    }

    private void init() {

    }

}
