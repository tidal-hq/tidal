package net.tidalhq.tidal.failsafe;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.macro.MacroManager;
import net.tidalhq.tidal.notification.Notifier;
import net.tidalhq.tidal.state.CompositeGameStateView;

public record FailsafeContext(
        CompositeGameStateView gameState,
        Notifier notifier
) {}
