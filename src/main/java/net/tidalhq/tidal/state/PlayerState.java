package net.tidalhq.tidal.state;

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

    private void onTick() {

    }

}
