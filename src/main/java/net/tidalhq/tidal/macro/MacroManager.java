package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.ClientEndTickEvent;
import net.tidalhq.tidal.feature.FeatureManager;
import net.tidalhq.tidal.registry.Registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MacroManager class responsible for high level selection and execution of a {@link Macro}.
 *
 * Responsible for registration to the {@link EventBus}, subscriptions are passed on to child {@link Macro} and {@link FeatureManager}.
 */
public class MacroManager {
    private static final Pattern DEATH_PATTERN = Pattern.compile("☠ You (?<reason>.+)");

    private final Registry<Macro> registry = new Registry<Macro>();
    private final EventBus eventBus;
    private final FeatureManager featureManager;

    private Macro activeMacro;
    private String activeMacroId;
    private boolean enabled;

    public MacroManager(MacroContext ctx, FeatureManager featureManager) {
        this.eventBus       = ctx.eventBus();
        this.featureManager = featureManager;
        eventBus.register(this);
    }

    /**
     * Registers a macro to the manager, allowing it to be enabled and disabled
     *
     * @param macro the macro object to register
     */
    public void register(Macro macro) {
        registry.put(macro);
    }

    @Subscribe
    public void onClientEndTickEvent(ClientEndTickEvent event) {
        if (enabled && activeMacro != null) {
            featureManager.tickWithMacro(activeMacro);
        }
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
     * Sets the managers current active {@link Macro} by string id, only one macro can be live at once so this is responsible for destruction.
     * For adding Macro objects to be enabled, see {@link #register}
     *
     * @param id string id of the macro to set active
     */
    public void setActiveMacro(String id) {
        Macro macro = registry.get(id).orElse(null);
        if (macro == null) return;

        if (activeMacro != null) {
            if (enabled) {
                activeMacro.onDisable();
            }
            featureManager.resetMacroPaused();
            eventBus.unregister(activeMacro);
        }

        activeMacro   = macro;
        activeMacroId = id;
        eventBus.register(activeMacro);
    }


    /**
     * Enable or disable the current {@link Macro} set by {@link #setActiveMacro}
     *
     * @param enabled enabled?
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled || activeMacro == null) return;

        if (enabled) {
            if (!featureManager.runPreMacroChecks(activeMacro)) return;
            boolean started = activeMacro.onEnable();
            if (!started) return;
        } else {
            activeMacro.onDisable();
            featureManager.resetMacroPaused();
        }

        this.enabled = enabled;
    }

    public String getActiveMacroId()       { return activeMacroId; }
    public Macro getActiveMacro()          { return activeMacro; }
    public boolean isEnabled()             { return enabled; }
}