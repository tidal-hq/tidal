package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.feature.impl.PestWarningFeature;
import net.tidalhq.tidal.macro.MacroContext;

public class FeatureManager {
    private final FeatureRegistry registry;

    public FeatureManager(MacroContext ctx) {
        this.registry = new FeatureRegistry();
        registerFeatures(ctx);
    }

    private void registerFeatures(MacroContext ctx) {
        register(new PestWarningFeature(ctx));
    }

    private void register(Feature feature) {
        registry.register(feature);
    }

    public boolean toggle(String id) {
        return registry.get(id).map(feature -> {
            boolean nowEnabled = !feature.isEnabled();
            feature.setEnabled(nowEnabled);
            if (nowEnabled) feature.onEnable();
            else feature.onDisable();
            return nowEnabled;
        }).orElse(false);
    }

    public void onTick() {
        registry.all().stream()
                .filter(Feature::isEnabled)
                .forEach(Feature::onTick);
    }

    public FeatureRegistry getRegistry() {
        return registry;
    }
}