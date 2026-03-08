package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;
import net.tidalhq.tidal.feature.Feature;

public class FeatureFailedEvent implements Event {
    private final Feature feature;
    private final String reason;

    public FeatureFailedEvent(Feature feature, String reason) {
        this.feature = feature;
        this.reason = reason;
    }

    public Feature getFeature() { return feature; }
    public String getReason() { return reason; }
}
