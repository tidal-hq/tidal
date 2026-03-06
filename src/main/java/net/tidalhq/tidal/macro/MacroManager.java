package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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