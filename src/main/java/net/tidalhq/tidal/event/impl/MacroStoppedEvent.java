package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;
import net.tidalhq.tidal.macro.Macro;

public class MacroStoppedEvent implements Event {

    private final Macro macro;

    public MacroStoppedEvent(Macro macro) {
        this.macro = macro;
    }

    public Macro getMacro() {
        return macro;
    }
}