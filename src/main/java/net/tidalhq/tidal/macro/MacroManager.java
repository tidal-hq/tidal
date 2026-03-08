package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.ClientTickEvent;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureManager;
import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroManager {
    private final Map<String, Macro> macros = new LinkedHashMap<>();
    private final EventBus eventBus;
    private final MacroContext ctx;
    private Macro activeMacro;
    private String activeMacroId;
    private boolean enabled;

    public MacroManager(MacroContext ctx) {
        this.ctx = ctx;
        this.eventBus = ctx.eventBus();
        eventBus.register(this);

        ctx.featureManager().setMacroCallback(new FeatureManager.MacroLifecycleCallback() {
            @Override
            public void onMacroPauseRequested(Feature requester) {
                if (activeMacro != null) activeMacro.pause();
            }

            @Override
            public void onMacroResumeRequested(Feature requester) {
                if (activeMacro != null) activeMacro.resume();
            }

            @Override
            public void onMacroStopRequested() {
                setEnabled(false);
            }
        });

        ctx.featureManager().toggleEnabled("pest_warning");
        ctx.featureManager().toggleEnabled("pest_remover");

        register("ssdsmushroom", new SShapeMushroomSDSMacro(ctx));
    }

    @Subscribe
    public void onClientTickEvent(ClientTickEvent event) {
        ctx.featureManager().onTick();

        if (!enabled || activeMacro == null) return;
        activeMacro.onTick();
    }

    @Subscribe
    public void onClientReceiveGameMessage(ClientReceiveGameMessageEvent event) {
        if (!enabled || activeMacro == null) return;

        Pattern pattern = Pattern.compile("☠ You (?<reason>.+)");
        Matcher matcher = pattern.matcher(event.getMessageContent());

        if (matcher.find()) {
            activeMacro.onDeath();
        }
    }

    private void register(String id, Macro macro) {
        macros.put(id, macro);
    }

    public void setActiveMacro(String id) {
        Macro macro = macros.get(id);
        if (macro == null) return;

        if (activeMacro != null) {
            eventBus.unregister(activeMacro);
        }

        activeMacro = macro;
        activeMacroId = id;
        eventBus.register(activeMacro);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        if (activeMacro == null) return;

        if (enabled) {
            boolean success = activeMacro.onEnable();
            if (!success) return;
            this.enabled = true;
            ctx.featureManager().onMacroStart();
        } else {
            this.enabled = false;
            ctx.featureManager().onMacroStop();
            activeMacro.onDisable();
        }
    }

    public String getActiveMacroId() { return activeMacroId; }
    public Macro getActiveMacro() { return activeMacro; }
    public boolean isEnabled() { return enabled; }
    public Map<String, Macro> getMacros() { return Collections.unmodifiableMap(macros); }
}