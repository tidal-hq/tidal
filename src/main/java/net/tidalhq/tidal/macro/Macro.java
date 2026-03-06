package net.tidalhq.tidal.macro;

import net.minecraft.client.MinecraftClient;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.state.ServerState;
import net.tidalhq.tidal.state.TablistState;
import net.tidalhq.tidal.util.BlockUtil;
import net.tidalhq.tidal.util.PlayerUtil;

public abstract class Macro {
    public static final MinecraftClient client = MinecraftClient.getInstance();
    private State currentState;
    private State lastState;

    private boolean pendingWarp = false;
    private int warpDelayTicks = 0;
    private boolean wasSneaking = false;

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
        if (!ServerState.getInstance().isConnectedToHypixel()) {
            return;
        }

        if (TablistState.getInstance().getCurrentLocation() != Location.GARDEN) {
            PlayerUtil.warp(Location.GARDEN);
        }

        if (!BlockUtil.isAtFarmStart(client.world, client.player)) {
            PlayerUtil.warp(Location.GARDEN);
        }
    }

    public void onDisable() {
        resetInputs();
    }

    public void onTick() {
        handleWarp();
        updateState();
        invokeState();

        boolean isWarpingOrDropping = getCurrentState() == State.WARPING || getCurrentState() == State.DROPPING;
        boolean shouldSneak = !isWarpingOrDropping && !client.player.isOnGround();

        if (shouldSneak) {
            client.options.sneakKey.setPressed(true);
            wasSneaking = true;
        }

        if (wasSneaking && !shouldSneak) {
            client.options.sneakKey.setPressed(false);
            wasSneaking = false;
        }
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
        PlayerUtil.warp(getLocation());

        changeState(State.NONE);
    }
}