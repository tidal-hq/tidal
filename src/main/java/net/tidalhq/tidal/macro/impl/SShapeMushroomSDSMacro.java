package net.tidalhq.tidal.macro.impl;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.MinecraftClient;
import net.tidalhq.tidal.Crop;
import net.tidalhq.tidal.macro.CorePhase;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.macro.MacroPhase;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.BlockUtil;
import net.tidalhq.tidal.util.InputUtil;

public class SShapeMushroomSDSMacro extends Macro {

    private MushroomPhase lastLaneDirection = MushroomPhase.MOVING_LEFT;

    private boolean hasLeftGround = false;

    public SShapeMushroomSDSMacro(MacroContext ctx) {
        super(ctx);
    }

    @Override
    public String getName() { return "S-Shape Mushroom (SDS)"; }

    @Override
    public Location getTargetLocation() { return Location.GARDEN; }

    @Override
    public Crop getTargetCrop() { return Crop.MUSHROOM; }

    @Override
    protected void onPhaseChanged(MacroPhase previous, MacroPhase next) {
        if (next != MushroomPhase.DROPPING) {
            hasLeftGround = false;
        }
    }

    @Override
    public void updatePhase() {
        MacroPhase phase = getPhase();

        if (phase == CorePhase.WARPING) return;

        if (phase == CorePhase.IDLE || phase == null) {
            setPhase(selectNextLane());
            return;
        }

        if (!(phase instanceof MushroomPhase mushroomPhase)) return;

        switch (mushroomPhase) {
            case SWITCHING_LANE -> {
                if (!BlockUtil.canWalkBackward()) {
                    setPhase(CorePhase.IDLE);
                }
            }
            case DROPPING -> tickDropping();
            case MOVING_RIGHT -> {
                if (!BlockUtil.canWalkRight()) {
                    setPhase(MushroomPhase.DROPPING);
                }
            }
            case MOVING_LEFT -> {
                if (BlockUtil.canWalkBackward()) {
                    setPhase(MushroomPhase.SWITCHING_LANE);
                } else if (!BlockUtil.canWalkLeft()) {
                    setPhase(CorePhase.IDLE);
                }
            }
        }
    }

    private void tickDropping() {
        if (!hasLeftGround) {
            hasLeftGround = !ctx.world().isPlayerOnGround();
            return;
        }
        if (ctx.world().isPlayerOnGround()) {
            setPhase(CorePhase.IDLE);
        }
    }

    private MacroPhase selectNextLane() {
        boolean tryRightFirst = lastLaneDirection == MushroomPhase.MOVING_LEFT;

        if (tryRightFirst) {
            if (BlockUtil.isRightCropReady() && BlockUtil.canWalkRight()) return commit(MushroomPhase.MOVING_RIGHT);
            if (BlockUtil.isLeftCropReady()  && BlockUtil.canWalkLeft())  return commit(MushroomPhase.MOVING_LEFT);
        } else {
            if (BlockUtil.isLeftCropReady()  && BlockUtil.canWalkLeft())  return commit(MushroomPhase.MOVING_LEFT);
            if (BlockUtil.isRightCropReady() && BlockUtil.canWalkRight()) return commit(MushroomPhase.MOVING_RIGHT);
        }

        if (BlockUtil.canWalkLeft())  return commit(MushroomPhase.MOVING_LEFT);
        if (BlockUtil.canWalkRight()) return commit(MushroomPhase.MOVING_RIGHT);

        return CorePhase.IDLE;
    }

    private MacroPhase commit(MushroomPhase direction) {
        lastLaneDirection = direction;
        return direction;
    }

    @Override
    public void applyInputs() {
        MacroPhase phase = getPhase();
        if (phase == null) return;

        GameOptions options = MinecraftClient.getInstance().options;

        InputUtil.release(
                options.leftKey,
                options.rightKey,
                options.backKey,
                options.forwardKey
        );

        if (phase instanceof MushroomPhase mushroomPhase) {
            switch (mushroomPhase) {
                case SWITCHING_LANE -> InputUtil.press(options.leftKey, options.backKey, options.attackKey);
                case DROPPING       -> { InputUtil.release(options.attackKey); InputUtil.press(options.rightKey); }
                case MOVING_LEFT    -> InputUtil.press(options.leftKey, options.attackKey);
                case MOVING_RIGHT   -> InputUtil.press(options.rightKey, options.backKey, options.attackKey);
            }
        } else {
            InputUtil.release(options.attackKey);
        }
    }
}