package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.notification.Notifier;
import net.tidalhq.tidal.pathfinder.RotationController;
import net.tidalhq.tidal.state.CompositeGameStateView;
import net.tidalhq.tidal.world.MinecraftWorldAccessor;


public record FeatureContext(
        CompositeGameStateView gameState,
        EventBus               eventBus,
        Notifier               notifier,
        MinecraftWorldAccessor worldAccessor,
        RotationController rotation
) {
    public FeatureContext(CompositeGameStateView gameState, EventBus eventBus,
                          Notifier notifier, MinecraftWorldAccessor worldAccessor) {
        this(gameState, eventBus, notifier, worldAccessor, new RotationController());
    }
}
