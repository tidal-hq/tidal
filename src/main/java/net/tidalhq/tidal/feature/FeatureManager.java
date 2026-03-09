package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.util.InputUtil;

import java.util.List;
import java.util.Optional;

public class FeatureManager {

    private final FeatureRegistry registry = new FeatureRegistry();
    private boolean macroPaused = false;

    public void register(Feature feature) {
        registry.register(feature);
    }

    public boolean toggle(String id) {
        Optional<Feature> feature = registry.get(id);
        if (feature.isEmpty()) return false;
        Feature f = feature.get();
        return setEnabled(f, !f.isEnabled());
    }

    public boolean setEnabled(String id, boolean enabled) {
        return registry.get(id)
                .map(f -> setEnabled(f, enabled))
                .orElse(false);
    }

    private boolean setEnabled(Feature feature, boolean enabled) {
        if (feature.isEnabled() == enabled) return enabled;
        feature.setEnabled(enabled);
        if (enabled) feature.onEnable();
        else feature.onDisable();
        return enabled;
    }

    public void onTick() {
        registry.all().stream()
                .filter(Feature::isEnabled)
                .forEach(f -> { f.onTick(); f.onNavigationTick(); });
    }

    public void tickWithMacro(Macro macro) {
        registry.all().stream()
                .filter(Feature::isEnabled)
                .forEach(f -> { f.onTick(); f.onNavigationTick(); });

        List<MacroLifecycleHook> hooks = enabledHooks();

        List<MacroLifecycleHook> pausingHooks = hooks.stream()
                .filter(h -> h.shouldPauseMacro(macro))
                .toList();

        if (!pausingHooks.isEmpty()) {
            if (!macroPaused) {
                macroPaused = true;
                InputUtil.reset();
                pausingHooks.forEach(h -> h.onMacroPaused(macro));
            }
            return;
        }

        if (macroPaused) {
            macroPaused = false;
            hooks.forEach(h -> h.onMacroResumed(macro));
        }

        macro.onTick();
    }

    public boolean runPreMacroChecks(Macro macro) {
        for (MacroLifecycleHook hook : enabledHooks()) {
            if (!hook.onBeforeMacroStart(macro)) {
                return false;
            }
        }
        return true;
    }

    public void resetMacroPaused() {
        macroPaused = false;
    }

    public boolean isMacroPaused() { return macroPaused; }

    public FeatureRegistry getRegistry() { return registry; }

    private List<MacroLifecycleHook> enabledHooks() {
        return registry.all().stream()
                .filter(Feature::isEnabled)
                .filter(f -> f instanceof MacroLifecycleHook)
                .map(f -> (MacroLifecycleHook) f)
                .toList();
    }
}