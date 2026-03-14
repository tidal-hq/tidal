package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;
import net.tidalhq.tidal.macro.Macro;

public class MacroStartedEvent implements Event {

    private final Macro macro;

    public MacroStartedEvent(Macro macro) {
        this.macro = macro;
    }

    public Macro getMacro() {
        return macro;
    }
}