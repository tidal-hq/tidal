package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.*;
import net.tidalhq.tidal.failsafe.FailsafeManager;
import net.tidalhq.tidal.feature.FeatureManager;
import net.tidalhq.tidal.pathfinder.RotationController;
import net.tidalhq.tidal.pathfinder.PathExecutor;
import net.tidalhq.tidal.registry.Registry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Subscribe
    public void onClientEndTick(ClientEndTickEvent event) {
        if (!enabled || activeMacro == null) return;
        featureManager.tickWithMacro(activeMacro);
    }

    @Subscribe
    public void onGameMessage(ClientReceiveGameMessageEvent event) {
        if (!enabled || activeMacro == null) return;
        Matcher matcher = DEATH_PATTERN.matcher(event.getMessageContent());
        if (matcher.find()) activeMacro.onDeath();
    }

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

    public RotationController getActiveRotation() {
        if (!enabled || activeMacro == null) return null;
        RotationController executorRotation = PathExecutor.getInstance().getRotation();
        if (executorRotation != null && executorRotation.hasTarget()) return executorRotation;
        return activeMacro.ctx.rotation();
    }

    public String getActiveMacroId()            { return activeMacroId; }
    public Macro getActiveMacro()               { return activeMacro; }
    public boolean isEnabled()                  { return enabled; }
    public FailsafeManager getFailsafeManager() { return failsafeManager; }
}