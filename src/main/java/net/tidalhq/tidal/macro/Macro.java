package net.tidalhq.tidal.macro;

import net.minecraft.client.MinecraftClient;

public abstract class Macro {
    public static final MinecraftClient client =  MinecraftClient.getInstance();

    public State currentState;
    public State lastState;

    public State getCurrentState() {
        return currentState;
    }
    public State getLastState() {
        return lastState;
    }

    public void changeState(State state) {
        currentState = state;
        lastState = state;
    }

    public abstract void updateState();
    public abstract void invokeState();

    public void onEnable() {

    }
    public void onTick() {}

    public abstract State calculateDirection();
}
