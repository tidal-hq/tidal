package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.event.impl.ClientEndTickEvent;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.macro.MacroManager;
import net.tidalhq.tidal.registry.Registry;
import net.tidalhq.tidal.util.InputUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * High level manager for high level selection and execution of a {@link Feature}, child of a {@link MacroManager}.
 * Contains a {@link net.tidalhq.tidal.registry.Registry} used for registering available features and retrieving them.
 */
public class FeatureManager {
    private final Registry<Feature> registry = new Registry<Feature>();
    private boolean macroPaused = false;

    public void register(Feature feature) {
        registry.put(feature);
    }

    /**
     * Toggles feature, same logic as {@link FeatureManager#setEnabled(String, boolean)} but with !feature.isEnabled() as arg.
     *
     * @param id string identifier of {@link Feature} object to toggle.
     * @return boolean success.
     */
    public boolean toggle(String id) {
        Optional<Feature> feature = registry.get(id);
        if (feature.isEmpty()) return false;
        Feature f = feature.get();
        return setEnabled(f, !f.isEnabled());
    }

    /**
     * @param id string identifier of target {@link Feature}.
     * @param enabled enable target?
     * @return boolean success.
     */
    public boolean setEnabled(String id, boolean enabled) {
        return registry.get(id)
                .map(f -> setEnabled(f, enabled))
                .orElse(false);
    }

    /**
     * Overloaded {@link FeatureManager#setEnabled(String, boolean)} which directly takes a {@link Feature} and performs actual enabling.
     * @param feature target {@link Feature} obj.
     * @param enabled enable target?
     * @return boolean success.
     */
    private boolean setEnabled(Feature feature, boolean enabled) {
        if (feature.isEnabled() == enabled) return enabled;
        feature.setEnabled(enabled);
        if (enabled) feature.onEnable();
        else feature.onDisable();
        return enabled;
    }

    /**
     * Ticks all enabled features outside a macro context.
     * Called from {@link MacroManager#onClientEndTickEvent} when no macro is active.
     *
     * @param event the tick event forwarded from {@link MacroManager}.
     */
    public void onClientEndTickEvent(ClientEndTickEvent event) {
        registry.getRegisteredObjects().stream()
                .filter(Feature::isEnabled)
                .forEach(Feature::onTick);
    }


    /**
     * Ticks all enabled features alongside the active macro, handling pause and resume transitions.
     * Called each tick by {@link MacroManager}
     *
     * @param macro the currently active, enabled macro
     */
    public void tickWithMacro(Macro macro) {
        registry.getRegisteredObjects().stream()
                .filter(Feature::isEnabled)
                .forEach(Feature::onTick);

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

    /**
     * Runs pre-start checks across all enabled {@link MacroLifecycleHook}s.
     * fails on the first veto, if any hook returns {@code false} the macro start is blocked immediately.
     *
     * @param macro the macro about to start.
     * @return {@code true} if all hooks approved; {@code false} if any vetoed.
     */
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

    public Registry<Feature> getRegistry() { return registry; }

    private List<MacroLifecycleHook> enabledHooks() {
        return registry.getRegisteredObjects().stream()
                .filter(Feature::isEnabled)
                .filter(f -> f instanceof MacroLifecycleHook)
                .map(f -> (MacroLifecycleHook) f)
                .toList();
    }
}