package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.*;
import net.tidalhq.tidal.failsafe.FailsafeManager;
import net.tidalhq.tidal.feature.FeatureManager;
import net.tidalhq.tidal.registry.Registry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages macro selection and the enabled/disabled lifecycle of the active {@link Macro}.
 *
 * MacroManager is responsible for exactly two things:
 *   1. Maintaining which macro is active and registered on the {@link EventBus}.
 *   2. Starting and stopping that macro in response to user commands or external stops.
 *
 * MacroManager does NOT tick FeatureManager or FailsafeManager. Each of those managers
 * subscribes to the EventBus independently and runs on its own cadence. This removes the
 * implicit tick-ordering dependency that previously lived here.
 *
 * Death detection remains here because it requires cross-cutting knowledge: the game message
 * event must be matched and translated into {@link Macro#onDeath()}, which is a macro-lifecycle
 * concern that doesn't belong in a Feature or Failsafe.
 */
public class MacroManager {
    private static final Pattern DEATH_PATTERN = Pattern.compile("☠ You (?<reason>.+)");

    private final Registry<Macro> registry = new Registry<>();
    private final EventBus eventBus;
    private final FeatureManager featureManager;
    private final FailsafeManager failsafeManager;

    private Macro activeMacro;
    private String activeMacroId;
    private boolean enabled;

    public MacroManager(MacroContext ctx, FeatureManager featureManager, FailsafeManager failsafeManager) {
        this.eventBus        = ctx.eventBus();
        this.featureManager  = featureManager;
        this.failsafeManager = failsafeManager;
        eventBus.register(this);
    }

    public void register(Macro macro) {
        registry.put(macro);
    }

    /**
     * Drives the active macro's tick. Features and Failsafes subscribe to ClientEndTickEvent
     * directly — this handler is only responsible for the macro's own onTick via FeatureManager,
     * which handles pause/resume logic.
     */
    @Subscribe
    public void onClientEndTick(ClientEndTickEvent event) {
        if (!enabled || activeMacro == null) return;
        featureManager.tickWithMacro(activeMacro);
    }

    @Subscribe
    public void onGameMessage(ClientReceiveGameMessageEvent event) {
        if (!enabled || activeMacro == null) return;

        Matcher matcher = DEATH_PATTERN.matcher(event.getMessageContent());
        if (matcher.find()) {
            activeMacro.onDeath();
        }
    }

    /**
     * Sets the active {@link Macro} by id. Disables and unregisters the current macro first
     * if one is running.
     *
     * @param id string id of the macro to activate
     */
    public void setActiveMacro(String id) {
        Macro macro = registry.get(id).orElse(null);
        if (macro == null) return;

        if (activeMacro != null) {
            if (enabled) {
                activeMacro.onDisable();
                eventBus.post(new MacroStoppedEvent(activeMacro));
            }
            eventBus.unregister(activeMacro);
        }

        activeMacro   = macro;
        activeMacroId = id;
        eventBus.register(activeMacro);
    }
    /**
     * Enables or disables the currently active macro.
     *
     * @param enabled desired state
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled || activeMacro == null) return;

        if (enabled) {
            if (!featureManager.runPreMacroChecks(activeMacro)) return;
            if (!activeMacro.onEnable()) return;
            eventBus.post(new MacroStartedEvent(activeMacro));
        } else {
            activeMacro.onDisable();
            eventBus.post(new MacroStoppedEvent(activeMacro));
        }

        this.enabled = enabled;
    }

    @Subscribe
    public void onFailsafeTrigger(FailsafeTriggerEvent event) {
        setEnabled(false);
    }

    public String getActiveMacroId()            { return activeMacroId; }
    public Macro getActiveMacro()               { return activeMacro; }
    public boolean isEnabled()                  { return enabled; }
    public FailsafeManager getFailsafeManager() { return failsafeManager; }
}