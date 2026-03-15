package net.tidalhq.tidal.failsafe;

import net.tidalhq.tidal.state.Location;

public final class LocationSanction {

    private final Location sanctionedLocation;
    private final String   reason;
    private boolean        consumed = false;

    public LocationSanction(Location sanctionedLocation, String reason) {
        this.sanctionedLocation = sanctionedLocation;
        this.reason             = reason;
    }

    public Location getSanctionedLocation() { return sanctionedLocation; }
    public String   getReason()             { return reason; }
    public boolean  isConsumed()            { return consumed; }

    public void consume() { consumed = true; }
}