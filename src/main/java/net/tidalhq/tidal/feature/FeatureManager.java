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
        return registry.get(id)
                .map(f -> setEnabled(f, !f.isEnabled()))
                .orElse(false);
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
                .forEach(Feature::onTick);
    }

    public void tickWithMacro(Macro macro) {
        registry.all().stream()
                .filter(Feature::isEnabled)
                .forEach(Feature::onTick);

        List<MacroLifecycleHook> hooks = enabledHooks();
        boolean anyWantsPause = hooks.stream().anyMatch(h -> h.shouldPauseMacro(macro));

        if (anyWantsPause) {
            if (!macroPaused) {
                macroPaused = true;
                InputUtil.reset();
                hooks.forEach(h -> h.onMacroPaused(macro));
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
        boolean allPassed = true;
        for (MacroLifecycleHook hook : enabledHooks()) {
            if (!hook.onBeforeMacroStart(macro)) {
                allPassed = false;
            }
        }
        return allPassed;
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