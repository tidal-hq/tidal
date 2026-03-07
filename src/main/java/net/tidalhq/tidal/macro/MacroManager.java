package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.ClientTickEvent;
import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;
import net.tidalhq.tidal.util.InputUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroManager {
    private final Map<String, Macro> macros = new LinkedHashMap<>();
    private final EventBus eventBus;
    private Macro activeMacro;
    private String activeMacroId;
    private boolean enabled;

    public MacroManager(MacroContext ctx) {
        this.eventBus = ctx.eventBus();
        eventBus.register(this);

        register("ssdsmushroom", new SShapeMushroomSDSMacro(ctx));
    }

    @Subscribe
    public void onClientTickEvent(ClientTickEvent event) {
        if (!enabled || activeMacro == null) return;
        activeMacro.onTick();
    }

    @Subscribe
    public void onClientReceiveGameMessage(ClientReceiveGameMessageEvent event) {
        if (!enabled || activeMacro == null) return;

        Pattern pattern = Pattern.compile("☠ You (?<reason>.+)");
        Matcher matcher = pattern.matcher(event.getMessageContent());

        if (matcher.find()) {
            if (enabled && activeMacro != null) {
                activeMacro.onDeath();
            }
        }
    }

    private void register(String id, Macro macro) {
        macros.put(id, macro);
        eventBus.register(macro);
    }

    public void setActiveMacro(String id) {
        Macro macro = macros.get(id);
        if (macro == null) return;

        activeMacro = macro;
        activeMacroId = id;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;

        if (activeMacro == null) return;

        if (enabled) {
            activeMacro.onEnable();
        } else {
            activeMacro.onDisable();
        }
    }

    public String getActiveMacroId() { return activeMacroId; }
    public Macro getActiveMacro() { return activeMacro; }
    public boolean isEnabled() { return enabled; }
    public Map<String, Macro> getMacros() { return Collections.unmodifiableMap(macros); }
}