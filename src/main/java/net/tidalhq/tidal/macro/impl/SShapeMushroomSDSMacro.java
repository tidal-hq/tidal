package net.tidalhq.tidal.macro.impl;

import net.minecraft.client.option.GameOptions;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.macro.State;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.BlockUtil;
import net.tidalhq.tidal.util.InputUtil;

@SuppressWarnings("resource")
public class SShapeMushroomSDSMacro extends Macro {
    private boolean leftGround = false;

    private State lastLaneDirection = State.LEFT;

    public SShapeMushroomSDSMacro(MacroContext ctx) {
        super(ctx);
    }

    @Override
    public String getName() {
        return "S-Shape Mushroom (SDS)";
    }

    @Override
    public Location getTargetLocation() {
        return Location.GARDEN;
    }

    @Override
    public void updateState() {
        if (getState() == null) {
            setState(State.NONE);
            return;
        }

        switch (getState()) {
            case SWITCHING_LANE:
                if (!BlockUtil.canWalkBackward()) {
                    setState(State.NONE);
                }
                break;

            case DROPPING:
                if (!leftGround) {
                    leftGround = !ctx.client().player.isOnGround();
                    break;
                }

                if (ctx.client().player.isOnGround()) {
                    setState(State.NONE);
                }
                break;

            case RIGHT:
                if (BlockUtil.canWalkRight()) {
                    setState(State.RIGHT);
                } else {
                    setState(State.DROPPING);
                }
                break;

            case LEFT:
                if (BlockUtil.canWalkBackward()) {
                    setState(State.SWITCHING_LANE);
                } else if (BlockUtil.canWalkLeft()) {
                    setState(State.LEFT);
                } else {
                    setState(State.NONE);
                }
                break;

            case NONE:
                setState(calculateDirection());
                break;
        }
    }

    @Override
    protected void onSetState(State newState) {
        if (newState != State.DROPPING) {
            leftGround = false;
        }
    }

    @Override
    public void onTick() {
        super.onTick();
    }
    public State calculateDirection() {
        boolean tryRightFirst = lastLaneDirection == State.LEFT;

        if (tryRightFirst) {
            if (BlockUtil.isRightCropReady() && BlockUtil.canWalkRight()) {
                lastLaneDirection = State.RIGHT;
                return State.RIGHT;
            }
            if (BlockUtil.isLeftCropReady() && BlockUtil.canWalkLeft()) {
                lastLaneDirection = State.LEFT;
                return State.LEFT;
            }
        } else {
            if (BlockUtil.isLeftCropReady() && BlockUtil.canWalkLeft()) {
                lastLaneDirection = State.LEFT;
                return State.LEFT;
            }
            if (BlockUtil.isRightCropReady() && BlockUtil.canWalkRight()) {
                lastLaneDirection = State.RIGHT;
                return State.RIGHT;
            }
        }

        if (BlockUtil.canWalkLeft()) {
            lastLaneDirection = State.LEFT;
            return State.LEFT;
        }
        if (BlockUtil.canWalkRight()) {
            lastLaneDirection = State.RIGHT;
            return State.RIGHT;
        }

        return State.NONE;
    }

    @Override
    public void invokeState() {
        if (getState() == null) return;

        GameOptions options = ctx.client().options;

        InputUtil.reset();

        switch (getState()) {
            case SWITCHING_LANE:
                InputUtil.press(
                        options.leftKey,
                        options.backKey,
                        options.attackKey
                );
                break;

            case DROPPING:
                InputUtil.press(
                        options.rightKey
                );
                break;

            case LEFT:
                InputUtil.press(
                        options.leftKey,
                        options.attackKey
                );
                break;

            case RIGHT:
                InputUtil.press(
                        options.rightKey,
                        options.backKey,
                        options.attackKey
                );
                break;
        }
    }

    @Override
    public void onDeath() {
        super.onDeath();
    }
}