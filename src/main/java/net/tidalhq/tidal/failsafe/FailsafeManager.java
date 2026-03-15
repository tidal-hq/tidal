package net.tidalhq.tidal.failsafe;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientEndTickEvent;
import net.tidalhq.tidal.event.impl.MacroStoppedEvent;
import net.tidalhq.tidal.registry.Registry;
import net.tidalhq.tidal.state.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all registered {@link Failsafe} instances.
 *
 * Also holds the active {@link LocationSanction} list. Features that need to warp
 * to a location outside the macro's expected area register a sanction here before
 * warping. Failsafes call {@link #isSanctioned(Location)} before firing.
 */
public class FailsafeManager {
    private final Registry<Failsafe>   registry   = new Registry<>();
    private final List<LocationSanction> sanctions = new ArrayList<>();

    public FailsafeManager() {
        EventBus.getInstance().register(this);
    }

    public void register(Failsafe failsafe) {
        registry.put(failsafe);
        EventBus.getInstance().register(failsafe);
    }

    /**
     * Register a location that a feature is intentionally about to warp to.
     * The failsafe will not fire while a matching, unconsumed sanction exists.
     */
    public void sanction(LocationSanction sanction) {
        sanctions.add(sanction);
    }

    /**
     * Check whether {@code location} is currently covered by an active sanction.
     * If it is, the matching sanction is consumed (one-use).
     */
    public boolean isSanctioned(Location location) {
        for (LocationSanction s : sanctions) {
            if (!s.isConsumed() && s.getSanctionedLocation().equals(location)) {
                s.consume();
                return true;
            }
        }
        return false;
    }

    public void clearSanctions() {
        sanctions.clear();
    }

    @Subscribe
    public void onClientEndTick(ClientEndTickEvent event) {
        registry.getRegisteredObjects().forEach(Failsafe::onTick);
    }

    @Subscribe
    public void onMacroStopped(MacroStoppedEvent event) {
        clearSanctions();
        registry.getRegisteredObjects().forEach(Failsafe::onMacroStopped);
    }

    public Registry<Failsafe> getRegistry() { return registry; }
}