package net.tidalhq.tidal.macro.impl;

import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.macro.State;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.BlockUtil;
import net.tidalhq.tidal.util.InputUtil;

@SuppressWarnings("resource")
public class SShapeMushroomSDSMacro extends Macro {
    private boolean leftGround = false;

    private final World world;
    private final PlayerEntity player;
    private final GameOptions options;

    public SShapeMushroomSDSMacro(MacroContext ctx) {
        super(ctx);

        this.world = ctx.client().world;
        this.player = ctx.client().player;
        this.options = ctx.client().options;
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
                if (!BlockUtil.canWalkBackward(this.world, this.player)) {
                    setState(State.NONE);
                }
                break;

            case DROPPING:
                if (!leftGround) {
                    leftGround = !player.isOnGround();
                    break;
                }

                if (player.isOnGround()) {
                    setState(State.NONE);
                }
                break;

            case RIGHT:
                if (BlockUtil.canWalkRight(world, player)) {
                    setState(State.RIGHT);
                } else {
                    setState(State.DROPPING);
                }
                break;

            case LEFT:
                if (BlockUtil.canWalkBackward(world, player)) {
                    setState(State.SWITCHING_LANE);
                } else if (BlockUtil.canWalkLeft(world, player)) {
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
        if (BlockUtil.isLeftCropReady(world, player)) {
            return State.LEFT;
        }

        if (BlockUtil.isRightCropReady(world, player)) {
            return State.RIGHT;
        }

        if (BlockUtil.canWalkLeft(world, player)) {
            return State.LEFT;
        }

        if (BlockUtil.canWalkRight(world, player)) {
            return State.RIGHT;
        }

        return State.NONE;
    }

    @Override
    public void invokeState() {
        if (getState() == null) return;

//        this.options.leftKey.setPressed(false);
//        this.options.rightKey.setPressed(false);
//        this.options.backKey.setPressed(false);
//        this.options.attackKey.setPressed(false);
        InputUtil.reset();

        switch (getState()) {
            case SWITCHING_LANE:
                InputUtil.press(
                        this.options.leftKey,
                        this.options.backKey,
                        this.options.attackKey
                );
                break;

            case DROPPING:
                InputUtil.press(
                        this.options.rightKey
                );
                break;

            case LEFT:
                InputUtil.press(
                        this.options.leftKey,
                        this.options.attackKey
                );
                break;

            case RIGHT:
                InputUtil.press(
                        this.options.rightKey,
                        this.options.backKey,
                        this.options.attackKey
                );
                break;
        }
    }

    @Override
    public void onDeath() {
        super.onDeath();
    }
}