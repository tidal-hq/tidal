package net.tidalhq.tidal.failsafe;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientEndTickEvent;
import net.tidalhq.tidal.event.impl.MacroStoppedEvent;
import net.tidalhq.tidal.registry.Registry;

/**
 * Manages all registered {@link Failsafe} instances.
 *
 * Subscribes to {@link ClientEndTickEvent} to poll each failsafe's {@link Failsafe#onTick()}.
 * Failsafes stop the macro by calling {@code ctx.macroManager().setEnabled(false)} directly —
 * MacroManager then posts {@link MacroStoppedEvent}, which this manager catches and forwards
 * to each failsafe's {@link Failsafe#onMacroStopped()} for per-run state cleanup.
 */
public class FailsafeManager {
    private final Registry<Failsafe> registry = new Registry<>();

    public FailsafeManager() {
        EventBus.getInstance().register(this);
    }

    public void register(Failsafe failsafe) {
        registry.put(failsafe);
        EventBus.getInstance().register(failsafe);
    }

    @Subscribe
    public void onClientEndTick(ClientEndTickEvent event) {
        registry.getRegisteredObjects().forEach(Failsafe::onTick);
    }

    @Subscribe
    public void onMacroStopped(MacroStoppedEvent event) {
        registry.getRegisteredObjects().forEach(Failsafe::onMacroStopped);
    }

    public Registry<Failsafe> getRegistry() {
        return registry;
    }
}