package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.notification.Notifier;
import net.tidalhq.tidal.pathfinder.RotationController;
import net.tidalhq.tidal.state.CompositeGameStateView;
import net.tidalhq.tidal.world.MinecraftWorldAccessor;

public record MacroContext(
        MinecraftWorldAccessor world,
        CompositeGameStateView gameState,
        EventBus               eventBus,
        Notifier               notifier,
        RotationController rotation
) {
    public MacroContext(MinecraftWorldAccessor world, CompositeGameStateView gameState,
                        EventBus eventBus, Notifier notifier) {
        this(world, gameState, eventBus, notifier, new RotationController());
    }
}