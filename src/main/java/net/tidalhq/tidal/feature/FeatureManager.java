package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.MacroStoppedEvent;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.macro.MacroManager;
import net.tidalhq.tidal.registry.Registry;
import net.tidalhq.tidal.util.InputUtil;

import java.util.List;
import java.util.Optional;

/**
 * Manages all registered {@link Feature} instances and their interaction with the active macro.
 *
 * FeatureManager does NOT subscribe to ClientEndTickEvent directly. It is driven by
 * {@link MacroManager#onClientEndTick}, which calls {@link #tickWithMacro(Macro)} when a macro
 * is running. This keeps the tick-ordering explicit: macro features always tick in lockstep with
 * the macro, not independently on a separate event subscription.
 *
 * FeatureManager subscribes to {@link MacroStoppedEvent} to clean up pause state when the macro
 * stops for any reason (user-initiated or failsafe-triggered).
 */
public class FeatureManager {
    private final Registry<Feature> registry = new Registry<>();
    private boolean macroPaused = false;

    public FeatureManager() {
        EventBus.getInstance().register(this);
    }

    public void register(Feature feature) {
        registry.put(feature);
        EventBus.getInstance().register(feature);
    }

    /**
     * Toggles a feature on/off by id.
     *
     * @param id string identifier of the target {@link Feature}.
     * @return boolean success.
     */
    public boolean toggle(String id) {
        Optional<Feature> feature = registry.get(id);
        if (feature.isEmpty()) return false;
        Feature f = feature.get();
        return setEnabled(f, !f.isEnabled());
    }

    /**
     * @param id      string identifier of target {@link Feature}.
     * @param enabled enable target?
     * @return boolean success.
     */
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

    /**
     * Ticks all enabled features alongside the active macro, handling pause and resume.
     * Called each tick by {@link MacroManager} — not via EventBus subscription — so that
     * feature ticks are guaranteed to happen in the same frame as the macro tick.
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
     * Fails on the first veto.
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

    @Subscribe
    public void onMacroStopped(MacroStoppedEvent event) {
        resetMacroPaused();
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