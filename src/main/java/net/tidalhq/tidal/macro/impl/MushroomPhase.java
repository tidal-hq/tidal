package net.tidalhq.tidal.macro.impl;


import net.tidalhq.tidal.macro.MacroPhase;

public enum MushroomPhase implements MacroPhase {

    MOVING_LEFT {
        @Override public boolean isRunning() { return true; }
    },

    MOVING_RIGHT {
        @Override public boolean isRunning() { return true; }
    },
    
    SWITCHING_LANE {
        @Override public boolean isRunning() { return true; }
    },
    
    DROPPING;
}