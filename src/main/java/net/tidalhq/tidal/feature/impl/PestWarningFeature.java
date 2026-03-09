package net.tidalhq.tidal.feature.impl;

import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.PestSpawnedEvent;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureContext;
import net.tidalhq.tidal.feature.MacroLifecycleHook;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.notification.Notification;

public class PestWarningFeature extends Feature implements MacroLifecycleHook {

    private static final int[] WARN_THRESHOLDS = {4, 5, 6, 7, 8};

    public PestWarningFeature(FeatureContext ctx) {
        super(ctx);
    }

    @Override public String getId()          { return "pest_warning"; }
    @Override public String getDisplayName() { return "Pest Warning"; }
    @Override public Category getCategory()  { return Category.FARMING; }

    @Override
    public void onEnable() {
        ctx.eventBus().register(this);
    }

    @Override
    public void onDisable() {
        ctx.eventBus().unregister(this);
    }

    @Override public boolean shouldPauseMacroExecution() { return false; }
    @Override public boolean shouldStartAtMacroStart() { return true; }

    @Subscribe
    public void onPestSpawned(PestSpawnedEvent event) {
        for (int threshold : WARN_THRESHOLDS) {
            if (event.getPrevious() < threshold && event.getCount() >= threshold) {
                warn(event.getCount());
                return;
            }
        }
    }

    private void warn(int count) {
        String msg = count + (count == WARN_THRESHOLDS[WARN_THRESHOLDS.length - 1] ? " pests - MAXIMUM!." : " pests in the garden!");
        ctx.notifier().send(msg, Notification.NotificationLevel.WARNING);
    }

    @Override
    public boolean shouldPauseMacro(Macro macro) {
//        return ctx.gameState().getPestsCount() >= PAUSE_THRESHOLD;
        return false;
    }
}