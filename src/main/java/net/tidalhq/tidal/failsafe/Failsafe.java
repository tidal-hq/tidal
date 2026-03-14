package net.tidalhq.tidal.failsafe;

import net.tidalhq.tidal.registry.Registerable;

public abstract class Failsafe implements Registerable {

    protected final FailsafeContext ctx;

    protected Failsafe(FailsafeContext ctx) {
        this.ctx = ctx;
    }

    public abstract void onTick();

    public void onMacroStopped() {}
}
