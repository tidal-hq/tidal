package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;

public class PestKilledEvent implements Event {
    private final int previous;
    private final int count;
    public PestKilledEvent(int previous, int count) {
        this.previous = previous;
        this.count = count;
    }

    public int getPrevious() {
        return previous;
    }

    public int getCount() {
        return count;
    }
}
