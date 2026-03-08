package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.FeatureFailedEvent;
import net.tidalhq.tidal.event.impl.FeatureTaskCompleteEvent;
import net.tidalhq.tidal.feature.impl.PestRemoverFeature;
import net.tidalhq.tidal.feature.impl.PestWarningFeature;
import net.tidalhq.tidal.macro.MacroContext;

public class FeatureManager {
    private final FeatureRegistry registry;

    public interface MacroLifecycleCallback {
        void onMacroPauseRequested(Feature requester);
        void onMacroResumeRequested(Feature requester);
        void onMacroStopRequested();
    }

    private MacroLifecycleCallback macroCallback;

    public FeatureManager(EventBus eventBus) {
        this.registry = new FeatureRegistry();
        eventBus.register(this);
    }

    @Subscribe
    public void onFeatureFailed(FeatureFailedEvent event) {
        if (macroCallback != null) macroCallback.onMacroStopRequested();
    }

    public void registerFeatures(MacroContext ctx) {
        register(new PestWarningFeature(ctx));
        register(new PestRemoverFeature(ctx));
    }

    private void register(Feature feature) {
        registry.register(feature);
    }

    public void setMacroCallback(MacroLifecycleCallback callback) {
        this.macroCallback = callback;
    }

    public boolean toggleEnabled(String id) {
        return registry.get(id).map(feature -> {
            if (feature.isEnabled()) feature.disable();
            else feature.enable();
            return feature.isEnabled();
        }).orElse(false);
    }

    public void onMacroStart() {
        registry.all().stream()
                .filter(f -> f.isEnabled() && f.shouldStartAtMacroStart())
                .forEach(Feature::start);
    }

    public void onMacroStop() {
        registry.all().stream()
                .filter(f -> !f.isIdle())
                .forEach(Feature::stop);
    }

    public void onMacroPause() {
        registry.all().stream()
                .filter(Feature::isRunning)
                .forEach(Feature::pause);
    }

    public void onMacroResume() {
        registry.all().stream()
                .filter(Feature::isPaused)
                .forEach(Feature::resume);
    }

    public void requestMacroPause(Feature feature) {
        if (macroCallback != null) macroCallback.onMacroPauseRequested(feature);
        feature.start();
    }

    @Subscribe
    public void onFeatureTaskComplete(FeatureTaskCompleteEvent event) {
        Feature feature = event.getFeature();
        feature.stop();
        if (feature.shouldPauseMacroExecution() && macroCallback != null) {
            macroCallback.onMacroResumeRequested(feature);
        }
    }

    public void onTick() {
        registry.all().stream()
                .filter(f -> f.isEnabled() && f.isRunning())
                .forEach(Feature::onTick);
    }

    public FeatureRegistry getRegistry() {
        return registry;
    }
}