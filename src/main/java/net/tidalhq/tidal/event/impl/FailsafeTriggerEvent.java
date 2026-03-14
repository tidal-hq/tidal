package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;
import net.tidalhq.tidal.failsafe.Failsafe;

public class FailsafeTriggerEvent implements Event {

    private final Failsafe failsafe;

    public FailsafeTriggerEvent(Failsafe failsafe) {
        this.failsafe = failsafe;
    }

    public Failsafe getFailsafe() {
        return failsafe;
    }
}