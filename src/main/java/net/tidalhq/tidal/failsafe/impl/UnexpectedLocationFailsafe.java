package net.tidalhq.tidal.failsafe.impl;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.*;
import net.tidalhq.tidal.failsafe.Failsafe;
import net.tidalhq.tidal.failsafe.FailsafeContext;
import net.tidalhq.tidal.state.Location;

import java.util.HashSet;
import java.util.Set;

public class UnexpectedLocationFailsafe extends Failsafe {

    private Location       expectedLocation;
    private boolean        armed;
    private final Set<Location> sanctionedLocations = new HashSet<>();

    public UnexpectedLocationFailsafe(FailsafeContext ctx) {
        super(ctx);
    }

    @Override public void onTick() {}

    @Override public String getName()        { return "Unexpected Location Failsafe"; }
    @Override public String getDescription() { return "Stops macro if the player moves to an unexpected location"; }
    @Override public String getId()          { return "unexpected_location"; }

    @Subscribe
    public void onLocationSanction(LocationSanctionEvent event) {
        sanctionedLocations.add(event.getLocation());
    }

    @Subscribe
    public void onMacroStarted(MacroStartedEvent event) {
        expectedLocation = event.getMacro().getTargetLocation();
        armed = false;
        sanctionedLocations.clear();
    }

    @Subscribe
    public void onClientEndTick(ClientEndTickEvent event) {
        if (expectedLocation == null) return;

        Location current = ctx.gameState().getCurrentLocation();
        if (current == null) return;

        if (!armed) {
            if (current.equals(expectedLocation)) armed = true;
            return;
        }

        if (current.equals(expectedLocation)) return;

        if (sanctionedLocations.remove(current)) {
            armed = false;
            return;
        }

        ctx.notifier().danger("[" + getName() + "] Expected " + expectedLocation.getName()
                + " but got " + current.getName() + " — stopping macro.");
        EventBus.getInstance().post(new FailsafeTriggerEvent(this));
    }

    @Subscribe
    public void onMacroStopped(MacroStoppedEvent event) {
        expectedLocation = null;
        armed = false;
        sanctionedLocations.clear();
    }
}