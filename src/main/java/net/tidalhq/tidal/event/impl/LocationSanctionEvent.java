package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;
import net.tidalhq.tidal.state.Location;

public class LocationSanctionEvent implements Event {
    private final Location location;
    private final String   reason;

    public LocationSanctionEvent(Location location, String reason) {
        this.location = location;
        this.reason   = reason;
    }

    public Location getLocation() { return location; }
    public String   getReason()   { return reason; }
}
