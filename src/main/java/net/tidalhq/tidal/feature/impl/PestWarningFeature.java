package net.tidalhq.tidal.feature.impl;

import net.minecraft.text.Text;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.PestSpawnedEvent;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.notification.Notification;

public class PestWarningFeature extends Feature {
    private static final int[] WARN_THRESHOLDS = {4, 5, 6, 7, 8};

    public PestWarningFeature(MacroContext ctx) {
        super(ctx);
    }

    @Override public String getId() { return "pest_warning"; }
    @Override public String getDisplayName() { return "Pest Warning"; }
    @Override public Category getCategory() { return Category.FARMING; }

    @Subscribe
    public void onPestCountIncreased(PestSpawnedEvent event) {
        if (!isEnabled()) return;

        for (int threshold : WARN_THRESHOLDS) {
            if (event.getPrevious() < threshold && event.getCount() >= threshold) {
                warn(event.getCount());
                return;
            }
        }
    }

    private void warn(int count) {
        String msg = count + (count == 8 ? " pests - MAXIMUM!" : " pests in the garden!");

        ctx.notifier().send(msg, Notification.NotificationLevel.WARNING);
    }
}
