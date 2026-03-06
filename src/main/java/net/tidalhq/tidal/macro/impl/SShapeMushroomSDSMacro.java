package net.tidalhq.tidal.macro.impl;

import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.macro.State;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.BlockUtil;

public class SShapeMushroomSDSMacro extends Macro {
    private boolean leftGround = false;
    private boolean isSneaking = false;

    @Override
    public Location getLocation() {
        return Location.GARDEN;
    }

    @Override
    public void updateState() {
        if (getCurrentState() == null) {
            changeState(State.NONE);
            return;
        }

        switch (getCurrentState()) {
            case SWITCHING_LANE:
                if (!BlockUtil.canWalkBackward(client.world, client.player)) {
                    changeState(State.NONE);
                }
                break;

            case DROPPING:
                if (!leftGround) {
                    leftGround = !client.player.isOnGround();
                    break;
                }

                if (client.player.isOnGround()) {
                    changeState(State.NONE);
                }
                break;

            case RIGHT:
                if (BlockUtil.canWalkRight(client.world, client.player)) {
                    changeState(State.RIGHT);
                } else {
                    changeState(State.DROPPING);
                }
                break;

            case LEFT:
                if (BlockUtil.canWalkBackward(client.world, client.player)) {
                    changeState(State.SWITCHING_LANE);
                } else if (BlockUtil.canWalkLeft(client.world, client.player)) {
                    changeState(State.LEFT);
                } else {
                    changeState(State.NONE);
                }
                break;

            case NONE:
                changeState(calculateDirection());
                break;
        }
    }

    @Override
    protected void onStateChanged(State newState) {
        if (newState != State.DROPPING) {
            leftGround = false;
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (client.player != null) {
            boolean shouldSneak = !client.player.isOnGround();
            if (shouldSneak != isSneaking) {
                isSneaking = shouldSneak;
                client.options.sneakKey.setPressed(shouldSneak);
            }
        }
    }

    @Override
    protected void resetInputs() {
        super.resetInputs();
        isSneaking = false;
    }

    public State calculateDirection() {
        if (BlockUtil.canWalkLeft(client.world, client.player)) {
            return State.LEFT;
        }

        if (BlockUtil.canWalkRight(client.world, client.player)) {
            return State.RIGHT;
        }

        return State.NONE;
    }

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;

        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.attackKey.setPressed(false);

        switch (getCurrentState()) {
            case SWITCHING_LANE:
                client.options.leftKey.setPressed(true);
                client.options.backKey.setPressed(true);
                client.options.attackKey.setPressed(true);
                break;

            case DROPPING:
                client.options.rightKey.setPressed(true);
                break;

            case LEFT:
                client.options.attackKey.setPressed(true);
                client.options.leftKey.setPressed(true);
                break;

            case RIGHT:
                client.options.attackKey.setPressed(true);
                client.options.rightKey.setPressed(true);
                client.options.backKey.setPressed(true);
                break;
        }
    }

    @Override
    public void onDeath() {
        super.onDeath();
//        changeState(State.NONE);
    }
}