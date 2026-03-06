package net.tidalhq.tidal.macro;

import net.minecraft.client.MinecraftClient;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.state.Location;

public abstract class Macro {
    public static final MinecraftClient client = MinecraftClient.getInstance();
    private State currentState;
    private State lastState;

    private boolean pendingWarp = false;
    private int warpDelayTicks = 0;

    public State getCurrentState() {
        return currentState;
    }

    public State getLastState() {
        return lastState;
    }

    public void changeState(State state) {
        lastState = currentState;
        currentState = state;
        onStateChanged(state);
    }

    public void onEnable() {
    }

    public void onDisable() {
        resetInputs();
    }

    public void onTick() {
        handleWarp();
        updateState();
        invokeState();
    }

    public void onDeath() {
        pendingWarp = true;
        warpDelayTicks = 40;
        changeState(State.WARPING);
        resetInputs();
    }

    public abstract void updateState();
    public abstract void invokeState();
    public abstract Location getLocation();

    protected void onStateChanged(State newState) {
    }

    protected void resetInputs() {
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.forwardKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
    }

    private void handleWarp() {
        if (pendingWarp) {
            warpDelayTicks--;
            if (warpDelayTicks <= 0) {
                pendingWarp = false;
                executeWarp();
            }
        }
    }

    protected void executeWarp() {
        String command = "warp " + getLocation().getName();
        client.getNetworkHandler().sendChatCommand(command);

        changeState(State.NONE);
    }
}