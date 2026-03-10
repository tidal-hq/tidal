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

public class SShapeMelonSDSMacro extends Macro {

    private MelonPhase lastLaneDirection = MelonPhase.MOVING_LEFT;

    public SShapeMelonSDSMacro(MacroContext ctx) {
        super(ctx);
    }

    @Override
    public String getName() { return "S-Shape Melon (SDS)"; }

    @Override
    public String getDescription() {
        return "S-Shape Melon (SDS)";
    }

    @Override
    public String getId() {
        return "s_sds_melon";
    }

    @Override
    public Location getTargetLocation() { return Location.GARDEN; }

    @Override
    public Crop getTargetCrop() { return Crop.MELON; }

    @Override
    protected void onPhaseChanged(MacroPhase previous, MacroPhase next) {
    }

    @Override
    public void updatePhase() {
        MacroPhase phase = getPhase();

        if (phase == CorePhase.WARPING) return;

        if (phase == CorePhase.IDLE || phase == null) {
            setPhase(selectNextLane());
            return;
        }

        if (!(phase instanceof MelonPhase melonPhase)) return;

        switch (melonPhase) {
            case SWITCHING_LANE -> {
                if (!BlockUtil.canWalkForward()) {
                    setPhase(selectNextLane());
                }
            }
            case MOVING_RIGHT -> {
                if (BlockUtil.canWalkForward()) {
                    setPhase(MelonPhase.SWITCHING_LANE);
                } else if (!BlockUtil.canWalkRight() && !BlockUtil.canWalkForward()) {
                    setPhase(MelonPhase.DROPPING);
                }
            }
            case MOVING_LEFT -> {
                if (BlockUtil.canWalkForward()) {
                    setPhase(MelonPhase.SWITCHING_LANE);
                } else if (!BlockUtil.canWalkLeft()) {
                    setPhase(CorePhase.IDLE);
                }
            }
            case DROPPING -> {
                if (!BlockUtil.canWalkLeft()) {
                    setPhase(CorePhase.IDLE);
                }
            }

        }
    }

    private MacroPhase selectNextLane() {
        boolean tryRightFirst = lastLaneDirection == MelonPhase.MOVING_LEFT;

        if (tryRightFirst) {
            if (BlockUtil.isRightCropReady() && BlockUtil.canWalkRight()) return commit(MelonPhase.MOVING_RIGHT);
            if (BlockUtil.isLeftCropReady()  && BlockUtil.canWalkLeft())  return commit(MelonPhase.MOVING_LEFT);
        } else {
            if (BlockUtil.isLeftCropReady()  && BlockUtil.canWalkLeft())  return commit(MelonPhase.MOVING_LEFT);
            if (BlockUtil.isRightCropReady() && BlockUtil.canWalkRight()) return commit(MelonPhase.MOVING_RIGHT);
        }

        if (BlockUtil.canWalkLeft())  return commit(MelonPhase.MOVING_LEFT);
        if (BlockUtil.canWalkRight()) return commit(MelonPhase.MOVING_RIGHT);

        return CorePhase.IDLE;
    }

    private MacroPhase commit(MelonPhase direction) {
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

        if (phase instanceof MelonPhase melonPhase) {
            switch (melonPhase) {
                case SWITCHING_LANE -> InputUtil.press(options.forwardKey);
                case MOVING_LEFT    -> InputUtil.press(options.leftKey, options.forwardKey, options.attackKey);
                case MOVING_RIGHT   -> InputUtil.press(options.rightKey, options.forwardKey, options.attackKey);
                case DROPPING -> InputUtil.press(options.rightKey);
            }
        } else {
            InputUtil.release(options.attackKey);
        }
    }
}