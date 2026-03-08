package net.tidalhq.tidal.event.impl;

import net.tidalhq.tidal.event.Event;
import net.tidalhq.tidal.feature.Feature;

public class FeatureTaskCompleteEvent implements Event {
    private final Feature feature;

    public FeatureTaskCompleteEvent(Feature feature) {
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }
}