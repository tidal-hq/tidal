package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.ClientTickEvent;
import net.tidalhq.tidal.feature.FeatureManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroManager {

    private static final Pattern DEATH_PATTERN = Pattern.compile("☠ You (?<reason>.+)");

    private final Map<String, Macro> macros = new LinkedHashMap<>();
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

    public void register(String id, Macro macro) {
        macros.put(id, macro);
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (enabled && activeMacro != null) {
            featureManager.tickWithMacro(activeMacro);
        } else {
            featureManager.onTick();
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

    public void setActiveMacro(String id) {
        Macro macro = macros.get(id);
        if (macro == null) return;

        if (activeMacro != null) {
            eventBus.unregister(activeMacro);
        }

        activeMacro   = macro;
        activeMacroId = id;
        eventBus.register(activeMacro);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled || activeMacro == null) return;

        if (enabled) {
            if (!featureManager.runPreMacroChecks(activeMacro)) return;
            boolean started = activeMacro.onEnable();
            if (!started) return;
        } else {
            activeMacro.onDisable();
        }

        this.enabled = enabled;
    }


    public String getActiveMacroId()       { return activeMacroId; }
    public Macro getActiveMacro()          { return activeMacro; }
    public boolean isEnabled()             { return enabled; }
    public Map<String, Macro> getMacros()  { return Collections.unmodifiableMap(macros); }
}