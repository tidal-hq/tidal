package net.tidalhq.tidal.macro;

public enum CorePhase implements MacroPhase {

    WARPING {
        @Override public boolean isIdle() { return true; }
    },

    IDLE {
        @Override public boolean isIdle() { return true; }
    };
}
