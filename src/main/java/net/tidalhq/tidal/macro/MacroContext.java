package net.tidalhq.tidal.macro;

import net.minecraft.client.MinecraftClient;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.feature.FeatureManager;
import net.tidalhq.tidal.notification.Notifier;
import net.tidalhq.tidal.state.ServerState;
import net.tidalhq.tidal.state.TablistState;

public record MacroContext(
        MinecraftClient client,
        ServerState serverState,
        TablistState tablistState,
        EventBus eventBus,
        Notifier notifier,
        FeatureManager featureManager
) {}