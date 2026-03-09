package net.tidalhq.tidal.macro;

public interface MacroPhase {

    default boolean isRunning() { return false; }

    default boolean isIdle() { return false; }
}

