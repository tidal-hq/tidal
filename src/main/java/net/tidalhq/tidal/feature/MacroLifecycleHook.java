package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.macro.Macro;

public interface MacroLifecycleHook {

    default boolean onBeforeMacroStart(Macro macro) { return true; }

    default boolean shouldPauseMacro(Macro macro) { return false; }

    default void onMacroPaused(Macro macro) {}

    default void onMacroResumed(Macro macro) {}
}