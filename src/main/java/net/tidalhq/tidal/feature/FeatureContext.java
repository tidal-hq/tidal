package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.notification.Notifier;
import net.tidalhq.tidal.state.CompositeGameStateView;

public record FeatureContext(
        CompositeGameStateView gameState,
        EventBus eventBus,
        Notifier notifier
) {}
